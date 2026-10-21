package service.branch;

import dao.ShowtimeDAO;
import dao.branch.BranchDAO;
import dao.branch.BranchOperatingHourDAO;
import dao.branch.RoomDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import dao.movie.MovieGenreDAO;
import dao.movie.MovieMediaDAO;
import model.branch.Branch;
import model.branch.BranchDetailDTO;
import model.branch.BranchOperatingHour;
import model.branch.Room;
import model.branch.RoomSummaryDTO;
import model.movie.Movie;
import model.movie.MovieSummaryDTO;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;
import util.TextUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manage Branch information + View Branch Details.
 *
 * BR-BR-01  Tên chi nhánh bắt buộc, tối đa 150 ký tự, không trùng.
 * BR-BR-02  Địa chỉ, thành phố, hotline bắt buộc. Hotline: số di động/cố định 10-11 số
 *           hoặc tổng đài 1800/1900.
 * BR-BR-03  Không ngưng hoạt động chi nhánh khi còn suất chiếu sắp tới.
 * BR-BR-04  Chi nhánh mới được tạo sẵn giờ mở cửa 09:00-23:00 cho cả tuần.
 * BR-BR-05  Trang chi tiết chỉ hiển thị chi nhánh đang hoạt động.
 */
public class BranchService {

    private static final Pattern HOTLINE = Pattern.compile("^(0\\d{9,10}|1[89]00\\d{4,6})$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final BranchDAO branchDAO = new BranchDAO();
    private final BranchOperatingHourDAO hourDAO = new BranchOperatingHourDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final MovieGenreDAO movieGenreDAO = new MovieGenreDAO();
    private final MovieMediaDAO movieMediaDAO = new MovieMediaDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    public List<Branch> list(String keyword, String status) throws SQLException, BusinessException {
        List<Branch> all = JdbcContext.query(con -> branchDAO.findAll(con, status));
        List<Branch> result = new ArrayList<>();
        for (Branch b : all) {
            if (TextUtil.matchesAny(keyword, b.getName(), b.getCity(), b.getAddress())) result.add(b);
        }
        return result;
    }

    public List<Branch> findActive() throws SQLException, BusinessException {
        return JdbcContext.query(branchDAO::findActive);
    }

    public Branch get(int branchId) throws SQLException, BusinessException {
        Branch b = JdbcContext.query(con -> branchDAO.findById(con, branchId));
        if (b == null) throw new NotFoundException("Không tìm thấy chi nhánh #" + branchId);
        return b;
    }

    /** Thêm mới (branchId = 0) hoặc cập nhật. Trả về id chi nhánh. */
    public int save(Branch b) throws SQLException, BusinessException {
        normalize(b);
        return JdbcContext.transaction(con -> {
            List<String> errors = new ArrayList<>();
            if (b.getName() == null) errors.add("Nhập tên chi nhánh.");
            else if (b.getName().length() > 150) errors.add("Tên chi nhánh tối đa 150 ký tự.");
            else if (branchDAO.existsByName(con, b.getName(), b.getBranchId()))
                errors.add("Đã có chi nhánh tên \"" + b.getName() + "\".");
            if (b.getAddress() == null) errors.add("Nhập địa chỉ.");
            else if (b.getAddress().length() > 255) errors.add("Địa chỉ tối đa 255 ký tự.");
            if (b.getCity() == null) errors.add("Nhập thành phố.");
            if (b.getHotline() == null) errors.add("Nhập hotline.");
            else if (!HOTLINE.matcher(b.getHotline()).matches())
                errors.add("Hotline phải là số 10-11 chữ số bắt đầu bằng 0, hoặc tổng đài 1800/1900.");
            if (b.getEmail() != null && !EMAIL.matcher(b.getEmail()).matches())
                errors.add("Email không hợp lệ.");
            if (!errors.isEmpty()) throw new BusinessException(errors);

            if (b.getBranchId() == 0) {
                b.setStatus(Branch.STATUS_ACTIVE);
                int id = branchDAO.insert(con, b);
                hourDAO.replaceForBranch(con, id, defaultWeek(id));
                return id;
            }
            if (branchDAO.findById(con, b.getBranchId()) == null)
                throw new NotFoundException("Không tìm thấy chi nhánh #" + b.getBranchId());
            branchDAO.update(con, b);
            return b.getBranchId();
        });
    }

    public void changeStatus(int branchId, String status) throws SQLException, BusinessException {
        if (!Branch.STATUS_ACTIVE.equals(status) && !Branch.STATUS_INACTIVE.equals(status))
            throw new BusinessException("Trạng thái không hợp lệ.");
        JdbcContext.transaction(con -> {
            Branch b = branchDAO.findById(con, branchId);
            if (b == null) throw new NotFoundException("Không tìm thấy chi nhánh #" + branchId);
            if (Branch.STATUS_INACTIVE.equals(status)) {
                int upcoming = showtimeDAO.countUpcomingByBranch(con, branchId);
                if (upcoming > 0)
                    throw new BusinessException("Không thể ngưng " + b.getName() + ": còn " + upcoming
                            + " suất chiếu sắp tới. Huỷ hoặc chuyển các suất chiếu trước.");
            }
            branchDAO.updateStatus(con, branchId, status);
            return null;
        });
    }

    /** View Branch Details (Guest, Customer, Branch). */
    public BranchDetailDTO getDetail(int branchId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Branch b = branchDAO.findById(con, branchId);
            if (b == null || !b.isActive())
                throw new NotFoundException("Chi nhánh không tồn tại hoặc đã ngưng hoạt động.");
            return buildDetail(con, b);
        });
    }

    /** Danh sách rạp cho khách (/branch): mọi chi nhánh đang hoạt động kèm giờ mở cửa, phòng, phim đang chiếu. */
    public List<BranchDetailDTO> listActiveDetails() throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            List<BranchDetailDTO> result = new ArrayList<>();
            for (Branch b : branchDAO.findActive(con)) result.add(buildDetail(con, b));
            return result;
        });
    }

    private BranchDetailDTO buildDetail(Connection con, Branch b) throws SQLException {
        int branchId = b.getBranchId();
        List<BranchOperatingHour> hours = hourDAO.findByBranch(con, branchId);

        List<RoomSummaryDTO> rooms = new ArrayList<>();
        for (RoomSummaryDTO r : roomDAO.findSummaries(con, branchId)) {
            if (Room.STATUS_ACTIVE.equals(r.getRoom().getStatus())) rooms.add(r);
        }

        LocalDate today = LocalDate.now();
        Map<Integer, List<String>> genres = movieGenreDAO.findGenreNamesPerMovie(con);
        Map<Integer, String> posters = movieMediaDAO.findPrimaryPosterUrls(con);
        List<MovieSummaryDTO> movies = new ArrayList<>();
        for (Movie m : movieDAO.findNowShowingAtBranch(con, branchId, today)) {
            movies.add(new MovieSummaryDTO(m, genres.get(m.getMovieId()), posters.get(m.getMovieId()), 0));
        }

        LocalDateTime now = LocalDateTime.now();
        return new BranchDetailDTO(b, hours, rooms, movies, isOpen(hours, now), now.getDayOfWeek().getValue());
    }

    public List<Branch> findOtherActive(int exceptBranchId) throws SQLException, BusinessException {
        List<Branch> result = new ArrayList<>();
        for (Branch b : findActive()) if (b.getBranchId() != exceptBranchId) result.add(b);
        return result;
    }

    // ------------------------------------------------------------------

    /** Đang mở cửa nếu giờ hiện tại thuộc khung hôm nay, hoặc thuộc phần qua đêm của hôm qua. */
    static boolean isOpen(List<BranchOperatingHour> hours, LocalDateTime now) {
        int today = now.getDayOfWeek().getValue();
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime t = now.toLocalTime();
        for (BranchOperatingHour h : hours) {
            if (h.getDayOfWeek() == today && h.isOpenAt(t)) return true;
            if (h.getDayOfWeek() == yesterday && h.isOvernight() && t.isBefore(h.getCloseTime())) return true;
        }
        return false;
    }

    private static List<BranchOperatingHour> defaultWeek(int branchId) {
        List<BranchOperatingHour> week = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            BranchOperatingHour h = new BranchOperatingHour(branchId, d);
            h.setOpenTime(LocalTime.of(9, 0));
            h.setCloseTime(LocalTime.of(23, 0));
            week.add(h);
        }
        return week;
    }

    private static void normalize(Branch b) {
        b.setName(clean(b.getName()));
        b.setAddress(clean(b.getAddress()));
        b.setCity(clean(b.getCity()));
        String hotline = clean(b.getHotline());
        b.setHotline(hotline == null ? null : hotline.replaceAll("[\\s.\\-]", ""));
        String email = clean(b.getEmail());
        b.setEmail(email == null ? null : email.toLowerCase());
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }
}
