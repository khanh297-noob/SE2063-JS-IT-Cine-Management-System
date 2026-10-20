package cinemanagement.service;

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

import cinemanagement.dao.AuditLogDAO;
import cinemanagement.dao.BookingDAO;
import cinemanagement.dao.BranchDAO;
import cinemanagement.dao.MovieDAO;
import cinemanagement.dao.PaymentDAO;
import cinemanagement.dao.RoomDAO;
import cinemanagement.dao.SeatHoldDAO;
import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.dao.TicketDAO;
import cinemanagement.model.Branch;
import cinemanagement.model.Movie;
import cinemanagement.model.OperatingHour;
import cinemanagement.model.OverlapResult;
import cinemanagement.model.Room;
import cinemanagement.model.ScheduleView;
import cinemanagement.model.Showtime;
import cinemanagement.model.User;
import cinemanagement.util.AppConfig;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DBContext;
import cinemanagement.util.DateTimeHelper;

public class ShowtimeService {
    public static final List<String> EDITABLE_STATUSES = List.of("Scheduled", "Open", "Closed");

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final SeatHoldDAO holdDAO = new SeatHoldDAO();
    private final AuditLogDAO audit = new AuditLogDAO();

    public static class ShowtimeInput {
        public int movieId;
        public int roomId;
        public LocalDate date;
        public LocalTime start;
        public BigDecimal price;
        public String status;
    }

    public int resolveBranchId(User actor, Integer requested) throws SQLException, BusinessException {
        if (actor.isAdmin()) {
            if (requested != null && branchDAO.findById(requested) != null) {
                return requested;
            }
            List<Branch> all = branchDAO.findAll();
            if (all.isEmpty()) {
                throw new BusinessException("Hệ thống chưa có chi nhánh nào.");
            }
            return all.get(0).getBranchId();
        }
        if (actor.getBranchId() == null) {
            throw new BusinessException("Tài khoản của bạn chưa được gán chi nhánh. Liên hệ quản trị viên.");
        }
        return actor.getBranchId();
    }

    private void ensureBranch(User actor, int branchId) throws BusinessException {
        if (actor.isAdmin()) {
            return;
        }
        if (!Objects.equals(actor.getBranchId(), branchId)) {
            throw new BusinessException("Bạn chỉ được quản lý suất chiếu thuộc chi nhánh của mình.");
        }
    }

    public List<Branch> getAllBranches() throws SQLException {
        return branchDAO.findAll();
    }

    public Branch getBranch(int branchId) throws SQLException {
        return branchDAO.findById(branchId);
    }

    public List<Room> getRooms(int branchId) throws SQLException {
        return roomDAO.findByBranch(branchId);
    }

    public List<Movie> getSchedulableMovies(int branchId) throws SQLException {
        return movieDAO.findSchedulableForBranch(branchId);
    }

    public int getBranchIdOfRoom(int roomId) throws SQLException {
        Room room = roomDAO.findById(roomId);
        return room == null ? 0 : room.getBranchId();
    }

    public OverlapResult checkOverlap(int roomId, int movieId, LocalDate date, LocalTime start, Integer excludeId)
            throws SQLException, BusinessException {
        Movie movie = movieDAO.findById(movieId);
        if (movie == null) {
            throw new BusinessException("Phim không tồn tại.");
        }
        try (Connection c = DBContext.getConnection()) {
            return checkOverlap(c, roomId, movie.getDuration(), date, start, excludeId);
        }
    }

    private OverlapResult checkOverlap(Connection c, int roomId, int duration, LocalDate date, LocalTime start,
                                       Integer excludeId) throws SQLException {
        LocalDateTime startAt = date.atTime(start);
        LocalDateTime endAt = startAt.plusMinutes(duration);
        int buffer = AppConfig.cleaningBufferMinutes();
        List<Showtime> conflicts = new ArrayList<>();
        for (Showtime o : showtimeDAO.findByRoomAround(c, roomId, date, excludeId)) {
            boolean overlap = startAt.isBefore(o.getEndAt().plusMinutes(buffer))
                    && o.getStartAt().isBefore(endAt.plusMinutes(buffer));
            if (overlap) {
                conflicts.add(o);
            }
        }
        return new OverlapResult(startAt, endAt, buffer, conflicts);
    }

    public List<Showtime> list(int branchId, LocalDate date, Integer roomId) throws SQLException {
        return showtimeDAO.search(branchId, date, date, roomId, null, true);
    }

    public Showtime get(User actor, int id) throws SQLException, BusinessException {
        Showtime s = showtimeDAO.findById(id);
        if (s == null) {
            throw new BusinessException("Suất chiếu không tồn tại.");
        }
        ensureBranch(actor, s.getBranchId());
        return s;
    }

    public int create(User actor, ShowtimeInput in, String ip) throws SQLException, BusinessException {
        Movie movie = requireMovie(in.movieId);
        validateBasic(in, movie);
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                Room room = lockRoom(c, actor, in.roomId);
                validateBranchRules(in, movie, room);
                OverlapResult ov = checkOverlap(c, room.getRoomId(), movie.getDuration(), in.date, in.start, null);
                if (!ov.isOk()) {
                    throw new BusinessException(ov.describe());
                }
                Showtime s = toShowtime(in, ov);
                int id = showtimeDAO.insert(c, s);
                c.commit();
                audit.log(actor.getUserId(), "Create showtime #" + id + " (" + movie.getTitle() + ", room " + room.getRoomId() + ")", ip);
                return id;
            } catch (SQLException | BusinessException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
    }

    public void update(User actor, int id, ShowtimeInput in, String ip) throws SQLException, BusinessException {
        Showtime cur = get(actor, id);
        if (cur.isCancelled()) {
            throw new BusinessException("Suất chiếu đã bị hủy, không thể chỉnh sửa.");
        }
        if (in.price == null || in.price.signum() <= 0) {
            throw new BusinessException("Giá vé cơ bản phải lớn hơn 0.");
        }
        String status = EDITABLE_STATUSES.contains(in.status) ? in.status : cur.getStatus();
        boolean scheduleChanged = cur.getMovieId() != in.movieId || cur.getRoomId() != in.roomId
                || !cur.getShowDate().equals(in.date) || !cur.getStartTime().equals(in.start);

        int sold = showtimeDAO.countTickets(id, true);
        if (scheduleChanged && sold > 0) {
            throw new BusinessException("Suất chiếu đã bán " + sold
                    + " vé nên không thể đổi phim/phòng/giờ chiếu. Chỉ được đổi giá (áp dụng cho vé bán sau) hoặc trạng thái.");
        }

        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                Showtime s;
                if (scheduleChanged) {
                    Movie movie = requireMovie(in.movieId);
                    validateBasic(in, movie);
                    Room room = lockRoom(c, actor, in.roomId);
                    validateBranchRules(in, movie, room);
                    OverlapResult ov = checkOverlap(c, room.getRoomId(), movie.getDuration(), in.date, in.start, id);
                    if (!ov.isOk()) {
                        throw new BusinessException(ov.describe());
                    }
                    s = toShowtime(in, ov);
                } else {
                    s = cur;
                }
                s.setShowtimeId(id);
                s.setBasePrice(in.price);
                s.setStatus(status);
                showtimeDAO.update(c, s);
                c.commit();
                audit.log(actor.getUserId(), "Update showtime #" + id, ip);
            } catch (SQLException | BusinessException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
    }

    public void assignRoom(User actor, int id, int roomId, String ip) throws SQLException, BusinessException {
        Showtime cur = get(actor, id);
        ShowtimeInput in = fromShowtime(cur);
        in.roomId = roomId;
        update(actor, id, in, ip);
    }

    public void assignMovie(User actor, int id, int movieId, String ip) throws SQLException, BusinessException {
        Showtime cur = get(actor, id);
        ShowtimeInput in = fromShowtime(cur);
        in.movieId = movieId;
        update(actor, id, in, ip);
    }

    public int cancel(User actor, int id, String ip) throws SQLException, BusinessException {
        Showtime cur = get(actor, id);
        if (cur.isCancelled()) {
            throw new BusinessException("Suất chiếu đã được hủy trước đó.");
        }
        if (cur.isStarted()) {
            throw new BusinessException("Suất chiếu đã bắt đầu hoặc đã chiếu xong, không thể hủy.");
        }
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                showtimeDAO.updateStatus(c, id, "Cancelled");
                holdDAO.deleteByShowtime(c, id);
                List<Integer> bookingIds = bookingDAO.findActiveBookingIdsByShowtime(c, id);
                for (int bookingId : bookingIds) {
                    BigDecimal total = bookingDAO.lockConfirmedTotal(c, bookingId);
                    if (total == null) {
                        continue;
                    }
                    ticketDAO.cancelByBooking(c, bookingId);
                    bookingDAO.markCancelled(c, bookingId, total);
                    paymentDAO.markRefunded(c, bookingId);
                }
                c.commit();
                audit.log(actor.getUserId(), "Cancel showtime #" + id + ", refunded " + bookingIds.size() + " booking(s)", ip);
                return bookingIds.size();
            } catch (SQLException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
    }

    public void delete(User actor, int id, String ip) throws SQLException, BusinessException {
        get(actor, id);
        if (showtimeDAO.countTickets(id, false) > 0) {
            throw new BusinessException("Suất chiếu đã phát sinh vé nên không được xóa (cần giữ lịch sử). Hãy dùng chức năng Hủy.");
        }
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                holdDAO.deleteByShowtime(c, id);
                showtimeDAO.delete(c, id);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
        audit.log(actor.getUserId(), "Delete showtime #" + id, ip);
    }

    public ScheduleView buildSchedule(int branchId, LocalDate from, int days) throws SQLException {
        LocalDate to = from.plusDays(days - 1L);
        ScheduleView view = new ScheduleView(branchDAO.findById(branchId), from, to);
        for (int i = 0; i < days; i++) {
            view.getDays().add(new ScheduleView.DayColumn(from.plusDays(i)));
        }
        List<Showtime> all = showtimeDAO.search(branchId, from, to, null, null, true);
        Map<String, ScheduleView.MovieStat> stats = new LinkedHashMap<>();
        int total = 0, cancelled = 0, sold = 0, seats = 0;
        for (Room room : roomDAO.findByBranch(branchId)) {
            ScheduleView.Row row = new ScheduleView.Row(room);
            for (int i = 0; i < days; i++) {
                LocalDate d = from.plusDays(i);
                List<Showtime> cell = new ArrayList<>();
                for (Showtime s : all) {
                    if (s.getRoomId() == room.getRoomId() && s.getShowDate().equals(d)) {
                        cell.add(s);
                    }
                }
                row.getCells().add(cell);
            }
            view.getRows().add(row);
        }
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
    }

    private Movie requireMovie(int movieId) throws SQLException, BusinessException {
        Movie m = movieDAO.findById(movieId);
        if (m == null) {
            throw new BusinessException("Phim không tồn tại.");
        }
        return m;
    }

    private Room lockRoom(Connection c, User actor, int roomId) throws SQLException, BusinessException {
        Room room = roomDAO.findById(c, roomId, true);
        if (room == null) {
            throw new BusinessException("Phòng chiếu không tồn tại.");
        }
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
        if (!movie.isSchedulable()) {
            throw new BusinessException("Phim \"" + movie.getTitle() + "\" đã ngừng chiếu, không thể xếp lịch (GB-01).");
        }
        if (movie.getReleaseDate() != null && in.date.isBefore(movie.getReleaseDate())) {
            throw new BusinessException("Ngày chiếu phải từ ngày khởi chiếu " + movie.getReleaseDateStr() + " trở đi.");
        }
        if (!in.date.atTime(in.start).isAfter(LocalDateTime.now())) {
            throw new BusinessException("Không thể xếp suất chiếu trong quá khứ.");
        }
        if (in.price == null || in.price.signum() <= 0) {
            throw new BusinessException("Giá vé cơ bản phải lớn hơn 0.");
        }
    }

    private void validateBranchRules(ShowtimeInput in, Movie movie, Room room) throws SQLException, BusinessException {
        Branch branch = branchDAO.findById(room.getBranchId());
        if (branch != null && !branch.isActive()) {
            throw new BusinessException("Chi nhánh " + branch.getName() + " đã ngừng hoạt động, không thể xếp suất chiếu.");
        }
        LocalDate[] window = movieDAO.findBranchWindow(movie.getMovieId(), room.getBranchId());
        if (window == null) {
            throw new BusinessException("Phim \"" + movie.getTitle() + "\" chưa được phân cho chi nhánh này.");
        }
        if (window[0] != null && in.date.isBefore(window[0])) {
            throw new BusinessException("Phim chỉ được chiếu tại chi nhánh từ ngày " + DateTimeHelper.format(window[0]) + ".");
        }
        if (window[1] != null && in.date.isAfter(window[1])) {
            throw new BusinessException("Phim chỉ được chiếu tại chi nhánh đến ngày " + DateTimeHelper.format(window[1]) + ".");
        }
        OperatingHour hour = branchDAO.findOperatingHour(room.getBranchId(), in.date.getDayOfWeek().getValue());
        if (hour != null) {
            if (hour.isClosed()) {
                throw new BusinessException("Chi nhánh đóng cửa vào " + DateTimeHelper.weekday(in.date) + ".");
            }
            if (!hour.isOpenAt(in.start)) {
                throw new BusinessException("Giờ bắt đầu phải trong giờ mở cửa của chi nhánh ("
                        + DateTimeHelper.format(hour.getOpenTime()) + "–" + DateTimeHelper.format(hour.getCloseTime()) + ").");
            }
        }
    }

    private Showtime toShowtime(ShowtimeInput in, OverlapResult ov) {
        Showtime s = new Showtime();
        s.setMovieId(in.movieId);
        s.setRoomId(in.roomId);
        s.setShowDate(in.date);
        s.setStartTime(in.start);
        s.setEndTime(ov.getEndAt().toLocalTime());
        s.setBasePrice(in.price);
        s.setStatus(EDITABLE_STATUSES.contains(in.status) ? in.status : "Open");
        return s;
    }

    private ShowtimeInput fromShowtime(Showtime s) {
        ShowtimeInput in = new ShowtimeInput();
        in.movieId = s.getMovieId();
        in.roomId = s.getRoomId();
        in.date = s.getShowDate();
        in.start = s.getStartTime();
        in.price = s.getBasePrice();
        in.status = s.getStatus();
        return in;
    }
}
