package service.movie;

import dao.booking.BookingDAO;
import dao.booking.SeatHoldDAO;
import dao.branch.BranchDAO;
import dao.branch.BranchOperatingHourDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import dao.movie.ShowtimeDAO;
import dao.pos.AuditLogDAO;
import model.User;
import model.branch.Branch;
import model.branch.BranchOperatingHour;
import model.movie.Movie;
import model.movie.OverlapResult;
import model.movie.ScheduleView;
import model.movie.Showtime;
import model.movie.ShowtimeRoom;
import util.BusinessException;
import util.DateTimeHelper;
import util.JdbcContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Quản lý suất chiếu (Showtime Management): tạo / sửa / huỷ / xoá suất, gán phòng, gán phim,
 * kiểm tra trùng lịch (BR-02) và lịch chiếu tổng hợp.
 * Giá suất chiếu tự tính theo phòng + ngày trong tuần (ShowtimePriceCalculator), quản lý không nhập tay.
 * Quản lý chi nhánh chỉ thao tác trên chi nhánh của mình (GB-05); Admin chọn được mọi chi nhánh.
 */
public class ShowtimeService {

    public static final List<String> EDITABLE_STATUSES = List.of("Scheduled", "Open", "Closed");
    /** Thời gian dọn phòng tối thiểu giữa 2 suất liên tiếp cùng phòng (BR-02). */
    public static final int CLEANING_BUFFER_MINUTES = 15;

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final BranchOperatingHourDAO hourDAO = new BranchOperatingHourDAO();
    private final SeatHoldDAO holdDAO = new SeatHoldDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /** Dữ liệu form tạo / sửa suất chiếu. Giá không có ở đây vì hệ thống tự tính. */
    public static class ShowtimeInput {
        public int movieId;
        public int roomId;
        public LocalDate date;
        public LocalTime start;
        public String status;
    }

    public static boolean isAdmin(User u) {
        return u != null && ("Administrator".equalsIgnoreCase(u.getRole()) || "Admin".equalsIgnoreCase(u.getRole()));
    }

    // ===================== Chi nhánh đang thao tác =====================

    /** Admin: chi nhánh yêu cầu (mặc định chi nhánh đầu tiên). Quản lý: luôn là chi nhánh của mình. */
    public int resolveBranchId(User actor, Integer requested) throws SQLException, BusinessException {
        if (isAdmin(actor)) {
            List<Branch> all = getAllBranches();
            for (Branch b : all) {
                if (requested != null && b.getBranchId() == requested) return requested;
            }
            if (all.isEmpty()) throw new BusinessException("Hệ thống chưa có chi nhánh nào.");
            return all.get(0).getBranchId();
        }
        if (actor.getBranchID() == null) {
            throw new BusinessException("Tài khoản của bạn chưa được gán chi nhánh. Liên hệ quản trị viên.");
        }
        return actor.getBranchID();
    }

    private void ensureBranch(User actor, int branchId) throws BusinessException {
        if (!isAdmin(actor) && !Objects.equals(actor.getBranchID(), branchId)) {
            throw new BusinessException("Bạn chỉ được quản lý suất chiếu thuộc chi nhánh của mình.");
        }
    }

    public List<Branch> getAllBranches() throws SQLException, BusinessException {
        return JdbcContext.query(con -> branchDAO.findAll(con, null));
    }

    public Branch getBranch(int branchId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> branchDAO.findById(con, branchId));
    }

    public List<ShowtimeRoom> getRooms(int branchId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> showtimeDAO.findRoomsByBranch(con, branchId));
    }

    public List<Movie> getSchedulableMovies(int branchId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> showtimeDAO.findSchedulableMovies(con, branchId));
    }

    public int getBranchIdOfRoom(int roomId) throws SQLException, BusinessException {
        ShowtimeRoom r = JdbcContext.query(con -> showtimeDAO.findRoom(con, roomId, false));
        return r == null ? 0 : r.getBranchId();
    }

    // ===================== Giá + trùng lịch =====================

    /** Giá suất chiếu cho phòng + ngày (form gọi để hiển thị trước khi lưu). */
    public BigDecimal previewPrice(int roomId, LocalDate date) throws SQLException, BusinessException {
        ShowtimeRoom room = JdbcContext.query(con -> showtimeDAO.findRoom(con, roomId, false));
        if (room == null) throw new BusinessException("Phòng chiếu không tồn tại.");
        return ShowtimePriceCalculator.basePrice(room.getRoomSurcharge(), date);
    }

    public OverlapResult checkOverlap(int roomId, int movieId, LocalDate date, LocalTime start, Integer excludeId)
            throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Movie movie = movieDAO.findById(con, movieId);
            if (movie == null) throw new BusinessException("Phim không tồn tại.");
            return checkOverlap(con, roomId, duration(movie), date, start, excludeId);
        });
    }

    /** BR-02: hai suất cùng phòng phải cách nhau tối thiểu CLEANING_BUFFER_MINUTES phút. */
    private OverlapResult checkOverlap(Connection con, int roomId, int duration, LocalDate date, LocalTime start,
                                       Integer excludeId) throws SQLException {
        LocalDateTime startAt = date.atTime(start);
        LocalDateTime endAt = startAt.plusMinutes(duration);
        int buffer = CLEANING_BUFFER_MINUTES;
        List<Showtime> conflicts = new ArrayList<>();
        for (Showtime o : showtimeDAO.findByRoomAround(con, roomId, date, excludeId)) {
            boolean overlap = startAt.isBefore(o.getEndAt().plusMinutes(buffer))
                    && o.getStartAt().isBefore(endAt.plusMinutes(buffer));
            if (overlap) conflicts.add(o);
        }
        return new OverlapResult(startAt, endAt, buffer, conflicts);
    }

    // ===================== Danh sách =====================

    public List<Showtime> list(int branchId, LocalDate date, Integer roomId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> showtimeDAO.search(con, branchId, date, date, roomId, null, true));
    }

    public Showtime get(User actor, int id) throws SQLException, BusinessException {
        Showtime s = JdbcContext.query(con -> showtimeDAO.findById(con, id));
        if (s == null) throw new BusinessException("Suất chiếu không tồn tại.");
        ensureBranch(actor, s.getBranchId());
        return s;
    }

    // ===================== Tạo / sửa =====================

    public int create(User actor, ShowtimeInput in, String ip) throws SQLException, BusinessException {
        int id = JdbcContext.transaction(con -> {
            Movie movie = requireMovie(con, in.movieId);
            validateBasic(in, movie);
            ShowtimeRoom room = lockRoom(con, actor, in.roomId);
            validateBranchRules(con, in, movie, room);
            OverlapResult ov = checkOverlap(con, room.getRoomId(), duration(movie), in.date, in.start, null);
            if (!ov.isOk()) throw new BusinessException(ov.describe());
            return showtimeDAO.insert(con, toShowtime(in, ov, room));
        });
        auditLogDAO.insert(actor.getUserId(), "SHOWTIME_CREATE|showtime=" + id + "|room=" + in.roomId, ip);
        return id;
    }

    public void update(User actor, int id, ShowtimeInput in, String ip) throws SQLException, BusinessException {
        Showtime cur = get(actor, id);
        if (cur.isCancelled()) {
            throw new BusinessException("Suất chiếu đã bị hủy, không thể chỉnh sửa.");
        }
        String status = EDITABLE_STATUSES.contains(in.status) ? in.status : cur.getStatus();
        boolean scheduleChanged = cur.getMovieId() != in.movieId || cur.getRoomId() != in.roomId
                || !cur.getShowDate().equals(in.date) || !cur.getStartTime().equals(in.start);
        if (scheduleChanged && cur.getSoldCount() > 0) {
            throw new BusinessException("Suất chiếu đã bán " + cur.getSoldCount()
                    + " vé nên không thể đổi phim/phòng/giờ chiếu. Chỉ được đổi trạng thái, hoặc hãy Hủy suất.");
        }
        JdbcContext.transaction(con -> {
            Showtime s;
            if (scheduleChanged) {
                Movie movie = requireMovie(con, in.movieId);
                validateBasic(in, movie);
                ShowtimeRoom room = lockRoom(con, actor, in.roomId);
                validateBranchRules(con, in, movie, room);
                OverlapResult ov = checkOverlap(con, room.getRoomId(), duration(movie), in.date, in.start, id);
                if (!ov.isOk()) throw new BusinessException(ov.describe());
                s = toShowtime(in, ov, room);
            } else {
                // Giữ lịch cũ, giá vẫn tính lại theo phụ thu phòng hiện tại (áp dụng cho vé bán sau)
                s = cur;
                ShowtimeRoom room = showtimeDAO.findRoom(con, cur.getRoomId(), false);
                s.setBasePrice(ShowtimePriceCalculator.basePrice(room.getRoomSurcharge(), cur.getShowDate()));
            }
            s.setShowtimeId(id);
            s.setStatus(status);
            showtimeDAO.update(con, s);
            return null;
        });
        auditLogDAO.insert(actor.getUserId(), "SHOWTIME_UPDATE|showtime=" + id, ip);
    }

    public void assignRoom(User actor, int id, int roomId, String ip) throws SQLException, BusinessException {
        ShowtimeInput in = fromShowtime(get(actor, id));
        in.roomId = roomId;
        update(actor, id, in, ip);
    }

    public void assignMovie(User actor, int id, int movieId, String ip) throws SQLException, BusinessException {
        ShowtimeInput in = fromShowtime(get(actor, id));
        in.movieId = movieId;
        update(actor, id, in, ip);
    }

    // ===================== Huỷ / xoá =====================

    /**
     * Huỷ suất (GB-08): suất chuyển Cancelled, nhả ghế đang giữ; đơn đã thanh toán bị huỷ + hoàn tiền,
     * đơn đang chờ thanh toán bị huỷ.
     * @return số đơn bị huỷ
     */
    public int cancel(User actor, int id, String ip) throws SQLException, BusinessException {
        Showtime cur = get(actor, id);
        if (cur.isCancelled()) throw new BusinessException("Suất chiếu đã được hủy trước đó.");
        if (cur.isStarted()) throw new BusinessException("Suất chiếu đã bắt đầu hoặc đã chiếu xong, không thể hủy.");
        List<Integer> bookingIds = JdbcContext.transaction(con -> {
            showtimeDAO.updateStatus(con, id, "Cancelled");
            holdDAO.deleteByShowtime(con, id);
            return showtimeDAO.findActiveBookingIds(con, id);
        });
        int count = 0;
        for (int bookingId : bookingIds) {
            if (bookingDAO.cancelCounterBooking(bookingId) || bookingDAO.cancelUnpaid(bookingId)) {
                count++;
            }
        }
        auditLogDAO.insert(actor.getUserId(), "SHOWTIME_CANCEL|showtime=" + id + "|bookings=" + count, ip);
        return count;
    }

    /** Xoá hẳn: chỉ khi chưa phát sinh vé nào (GB-16, cần giữ lịch sử). */
    public void delete(User actor, int id, String ip) throws SQLException, BusinessException {
        get(actor, id);
        JdbcContext.transaction(con -> {
            if (showtimeDAO.countTickets(con, id, false) > 0) {
                throw new BusinessException("Suất chiếu đã phát sinh vé nên không được xóa (cần giữ lịch sử). Hãy dùng chức năng Hủy.");
            }
            holdDAO.deleteByShowtime(con, id);
            showtimeDAO.delete(con, id);
            return null;
        });
        auditLogDAO.insert(actor.getUserId(), "SHOWTIME_DELETE|showtime=" + id, ip);
    }

    // ===================== Lịch tổng hợp =====================

    public ScheduleView buildSchedule(int branchId, LocalDate from, int days) throws SQLException, BusinessException {
        LocalDate to = from.plusDays(days - 1L);
        return JdbcContext.query(con -> {
            ScheduleView view = new ScheduleView(branchDAO.findById(con, branchId), from, to);
            for (int i = 0; i < days; i++) {
                view.getDays().add(new ScheduleView.DayColumn(from.plusDays(i)));
            }
            List<Showtime> all = showtimeDAO.search(con, branchId, from, to, null, null, true);
            for (ShowtimeRoom room : showtimeDAO.findRoomsByBranch(con, branchId)) {
                ScheduleView.Row row = new ScheduleView.Row(room);
                for (int i = 0; i < days; i++) {
                    LocalDate d = from.plusDays(i);
                    List<Showtime> cell = new ArrayList<>();
                    for (Showtime s : all) {
                        if (s.getRoomId() == room.getRoomId() && s.getShowDate().equals(d)) cell.add(s);
                    }
                    row.getCells().add(cell);
                }
                view.getRows().add(row);
            }
            Map<String, ScheduleView.MovieStat> stats = new LinkedHashMap<>();
            int total = 0, cancelled = 0, sold = 0, seats = 0;
            for (Showtime s : all) {
                if (s.isCancelled()) {
                    cancelled++;
                    continue;
                }
                total++;
                sold += s.getSoldCount();
                seats += s.getSeatCount();
                stats.computeIfAbsent(s.getMovieTitle(), ScheduleView.MovieStat::new).add(s);
            }
            view.setTotalShowtimes(total);
            view.setCancelledShowtimes(cancelled);
            view.setTotalSold(sold);
            view.setTotalSeats(seats);
            view.getMovieStats().addAll(stats.values());
            return view;
        });
    }

    // ===================== Kiểm tra dữ liệu =====================

    private int duration(Movie m) {
        return m.getDuration() == null ? 0 : m.getDuration();
    }

    private Movie requireMovie(Connection con, int movieId) throws SQLException, BusinessException {
        Movie m = movieDAO.findById(con, movieId);
        if (m == null) throw new BusinessException("Phim không tồn tại.");
        return m;
    }

    private ShowtimeRoom lockRoom(Connection con, User actor, int roomId) throws SQLException, BusinessException {
        ShowtimeRoom room = showtimeDAO.findRoom(con, roomId, true);
        if (room == null) throw new BusinessException("Phòng chiếu không tồn tại.");
        ensureBranch(actor, room.getBranchId());
        if (!room.isActive()) {
            String why = "Maintenance".equals(room.getStatus()) ? "đang bảo trì" : "đã ngừng hoạt động";
            throw new BusinessException("Phòng " + room.getRoomName() + " " + why + ", không thể xếp suất chiếu (BR-01).");
        }
        if (room.getSeatCount() == 0) {
            throw new BusinessException("Phòng " + room.getRoomName() + " chưa được cấu hình sơ đồ ghế.");
        }
        return room;
    }

    private void validateBasic(ShowtimeInput in, Movie movie) throws BusinessException {
        if (in.date == null || in.start == null) {
            throw new BusinessException("Ngày chiếu và giờ bắt đầu là bắt buộc.");
        }
        if (!(movie.isNowShowing() || movie.isComingSoon())) {
            throw new BusinessException("Phim \"" + movie.getTitle() + "\" đã ngừng chiếu, không thể xếp lịch (GB-01).");
        }
        if (movie.getReleaseDate() != null && in.date.isBefore(movie.getReleaseDate())) {
            throw new BusinessException("Ngày chiếu phải từ ngày khởi chiếu "
                    + DateTimeHelper.format(movie.getReleaseDate()) + " trở đi.");
        }
        if (!in.date.atTime(in.start).isAfter(LocalDateTime.now())) {
            throw new BusinessException("Không thể xếp suất chiếu trong quá khứ.");
        }
    }

    private void validateBranchRules(Connection con, ShowtimeInput in, Movie movie, ShowtimeRoom room)
            throws SQLException, BusinessException {
        Branch branch = branchDAO.findById(con, room.getBranchId());
        if (branch != null && !branch.isActive()) {
            throw new BusinessException("Chi nhánh " + branch.getName() + " đã ngừng hoạt động, không thể xếp suất chiếu.");
        }
        if (!movieBranchDAO.isAvailable(con, movie.getMovieId(), room.getBranchId(), in.date)) {
            throw new BusinessException("Phim \"" + movie.getTitle()
                    + "\" chưa được phân cho chi nhánh này hoặc ngày chiếu nằm ngoài thời gian chiếu tại chi nhánh.");
        }
        int dow = in.date.getDayOfWeek().getValue();
        for (BranchOperatingHour h : hourDAO.findByBranch(con, room.getBranchId())) {
            if (h.getDayOfWeek() != dow) continue;
            if (h.isClosed()) {
                throw new BusinessException("Chi nhánh đóng cửa vào " + h.getDayName() + ".");
            }
            if (!h.isOpenAt(in.start)) {
                throw new BusinessException("Giờ bắt đầu phải trong giờ mở cửa của chi nhánh ("
                        + DateTimeHelper.format(h.getOpenTime()) + " - " + DateTimeHelper.format(h.getCloseTime()) + ").");
            }
        }
    }

    private Showtime toShowtime(ShowtimeInput in, OverlapResult ov, ShowtimeRoom room) {
        Showtime s = new Showtime();
        s.setMovieId(in.movieId);
        s.setRoomId(in.roomId);
        s.setShowDate(in.date);
        s.setStartTime(in.start);
        s.setEndTime(ov.getEndAt().toLocalTime());
        s.setBasePrice(ShowtimePriceCalculator.basePrice(room.getRoomSurcharge(), in.date));
        s.setStatus(EDITABLE_STATUSES.contains(in.status) ? in.status : "Open");
        return s;
    }

    private ShowtimeInput fromShowtime(Showtime s) {
        ShowtimeInput in = new ShowtimeInput();
        in.movieId = s.getMovieId();
        in.roomId = s.getRoomId();
        in.date = s.getShowDate();
        in.start = s.getStartTime();
        in.status = s.getStatus();
        return in;
    }
}
