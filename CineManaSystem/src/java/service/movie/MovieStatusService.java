package service.movie;

import dao.ShowtimeDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import dao.movie.MovieGenreDAO;
import dao.movie.MovieMediaDAO;
import model.movie.Movie;
import model.movie.MovieMedia;
import model.movie.MovieStatusDTO;
import model.notification.Notification;
import service.notification.NotificationService;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Movie Status.
 *
 * Chuyển trạng thái hợp lệ:
 *   Coming Soon      -> Now Showing, End of Screening (huỷ chiếu)
 *   Now Showing      -> End of Screening
 *   End of Screening -> Now Showing (chiếu lại)
 *
 * BR-MS-01  "Now Showing" khi: đã tới ngày khởi chiếu, có ít nhất 1 thể loại, 1 poster và 1 chi nhánh
 *           đang chiếu (chi nhánh đang hoạt động và hôm nay nằm trong khung ngày chiếu của chi nhánh đó).
 *           Phim không có thể loại (vd thể loại duy nhất bị xoá) không được phát sóng.
 * BR-MS-02  Sang "End of Screening": không còn suất chiếu sắp tới.
 * BR-MS-03  Đối soát tự động (khi khởi động, mỗi ngày lúc 00:01, mỗi lần mở trang quản lý phim / thể loại,
 *           sau khi đổi trạng thái chi nhánh): phim "Now Showing" không còn chi nhánh đang chiếu / thể loại /
 *           poster (vd chi nhánh bị xoá / ngưng, hết khung ngày chiếu) -> về "Coming Soon", giống BR-GE-02.
 *           Còn suất chiếu sắp tới thì không tự chuyển, chỉ báo Admin.
 *           Ngày khởi chiếu KHÔNG làm trạng thái tự đổi: sửa ngày khởi chiếu thoải mái, trạng thái do Admin quyết.
 * Thông báo (gửi sau khi đã commit, lỗi gửi không ảnh hưởng đổi trạng thái):
 *   - Phim sang "Now Showing" -> mọi khách hàng ("Phim mới đang chiếu" / "Phim chiếu lại").
 *   - Phim bị đối soát chuyển về "Coming Soon" -> mọi Admin.
 *   - Mọi lần đổi trạng thái (tay hoặc đối soát) -> Quản lý các chi nhánh đang gán phim đó.
 */
public class MovieStatusService {

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            Movie.STATUS_COMING_SOON, List.of(Movie.STATUS_NOW_SHOWING, Movie.STATUS_ENDED),
            Movie.STATUS_NOW_SHOWING, List.of(Movie.STATUS_ENDED),
            Movie.STATUS_ENDED, List.of(Movie.STATUS_NOW_SHOWING));

    private final MovieDAO movieDAO = new MovieDAO();
    private final MovieGenreDAO movieGenreDAO = new MovieGenreDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final MovieMediaDAO movieMediaDAO = new MovieMediaDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final NotificationService notificationService = new NotificationService();

    public MovieStatusDTO getStatus(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> load(con, movieId));
    }

    public void changeStatus(int movieId, String target) throws SQLException, BusinessException {
        Movie before = JdbcContext.transaction(con -> {
            MovieStatusDTO s = load(con, movieId);
            Movie m = s.getMovie();
            if (target == null || !s.getAllowedTargets().contains(target))
                throw new BusinessException("Không thể chuyển \"" + m.getTitle() + "\" từ "
                        + m.getStatus() + " sang " + target + ".");

            List<String> errors = new ArrayList<>();
            if (Movie.STATUS_NOW_SHOWING.equals(target)) {
                if (!s.isReleased()) errors.add("Chưa tới ngày khởi chiếu " + m.getReleaseDate() + ".");
                if (s.getGenreCount() == 0) errors.add("Phim chưa có thể loại nên không được phát sóng. Thêm thể loại cho phim trước.");
                if (s.getBranchCount() == 0)
                    errors.add("Chưa có chi nhánh đang chiếu: cần ít nhất 1 chi nhánh đang hoạt động và hôm nay nằm trong khung ngày chiếu.");
                if (s.getPosterCount() == 0) errors.add("Chưa có poster.");
            } else if (Movie.STATUS_ENDED.equals(target) && s.getUpcomingShowtimes() > 0) {
                errors.add("Còn " + s.getUpcomingShowtimes() + " suất chiếu sắp tới. Huỷ các suất chiếu trước.");
            }
            if (!errors.isEmpty()) throw new BusinessException(errors);

            movieDAO.updateStatus(con, movieId, target);
            return m;
        });
        if (Movie.STATUS_NOW_SHOWING.equals(target)) {
            boolean rerun = before.isEnded();
            notificationService.notifyAllCustomers(Notification.TYPE_MOVIE,
                    (rerun ? "Phim chiếu lại: " : "Phim mới đang chiếu: ") + before.getTitle(),
                    "Đặt vé ngay tại các rạp CineGo.",
                    "/movies/detail?id=" + movieId);
        }
        notificationService.notifyManagersOfMovie(movieId, Notification.TYPE_MOVIE_STATUS,
                "Phim \"" + before.getTitle() + "\" chuyển sang " + statusLabel(target),
                "Admin đã đổi trạng thái từ " + statusLabel(before.getStatus()) + " sang " + statusLabel(target)
                        + ". Phim đang được gán cho chi nhánh của bạn.",
                "/movies/detail?id=" + movieId);
    }

    /** Tên trạng thái hiển thị trong thông báo. */
    private static String statusLabel(String status) {
        if (Movie.STATUS_NOW_SHOWING.equals(status)) return "Đang chiếu";
        if (Movie.STATUS_COMING_SOON.equals(status)) return "Sắp chiếu";
        if (Movie.STATUS_ENDED.equals(status)) return "Ngừng chiếu";
        return status;
    }

    /** Kết quả đối soát BR-MS-03, để báo lại cho Admin / ghi log. */
    public static final class RuleCheckResult {
        private final List<String> demoted = new ArrayList<>();
        private final List<String> needsAttention = new ArrayList<>();
        /** Phim vừa bị chuyển về "Coming Soon": [movieId, tên, lý do] để gửi thông báo cho Admin. */
        private final List<Object[]> demotedMovies = new ArrayList<>();

        /** Phim đã chuyển về "Coming Soon", kèm lý do. */
        public List<String> getDemoted() { return demoted; }
        /** Phim vi phạm nhưng còn suất chiếu sắp tới nên không tự chuyển. */
        public List<String> getNeedsAttention() { return needsAttention; }
        /** true nếu lần đối soát này không đổi dữ liệu và không có gì cần Admin xử lý gấp. */
        public boolean isClean() { return demoted.isEmpty() && needsAttention.isEmpty(); }

        /** Thông báo cho Admin về các thay đổi / việc cần xử lý; null nếu isClean(). */
        public String toMessage() {
            if (isClean()) return null;
            StringBuilder msg = new StringBuilder();
            if (!demoted.isEmpty())
                msg.append("Đã chuyển ").append(demoted.size())
                   .append(" phim từ Now Showing về Coming Soon vì không còn đủ điều kiện đang chiếu: ")
                   .append(String.join("; ", demoted)).append(". ");
            if (!needsAttention.isEmpty())
                msg.append("Cần xử lý: ").append(String.join("; ", needsAttention))
                   .append(". Phim đang chiếu nhưng thiếu điều kiện, chưa tự chuyển vì còn suất chiếu.");
            return msg.toString().trim();
        }
    }

    /** BR-MS-03: phim "Now Showing" không còn chi nhánh đang chiếu / thể loại / poster -> về "Coming Soon". */
    public RuleCheckResult enforceStatusRules() throws SQLException, BusinessException {
        RuleCheckResult checked = JdbcContext.transaction(con -> {
            RuleCheckResult result = new RuleCheckResult();
            LocalDate today = LocalDate.now();

            for (Movie m : movieDAO.findAll(con, Movie.STATUS_NOW_SHOWING, null)) {
                List<String> missing = missingShowingData(con, m, today);
                if (missing.isEmpty()) continue;
                String reason = "\"" + m.getTitle() + "\" (" + String.join(", ", missing);
                int upcoming = showtimeDAO.countUpcomingByMovie(con, m.getMovieId());
                if (upcoming > 0) {
                    result.needsAttention.add(reason + ", còn " + upcoming + " suất chiếu sắp tới)");
                } else {
                    movieDAO.updateStatus(con, m.getMovieId(), Movie.STATUS_COMING_SOON);
                    result.demoted.add(reason + ")");
                    result.demotedMovies.add(new Object[]{m.getMovieId(), m.getTitle(), String.join(", ", missing)});
                }
            }
            return result;
        });
        // Chỉ báo khi trạng thái vừa đổi (một lần), không báo lại các phim "Cần xử lý" mỗi lần đối soát
        for (Object[] d : checked.demotedMovies) {
            notificationService.notifyAdmins(Notification.TYPE_MOVIE_STATUS,
                    "Phim bị chuyển về Sắp chiếu: " + d[1],
                    "Lý do: " + d[2] + ". Bổ sung dữ liệu rồi chuyển lại sang Đang chiếu.",
                    "/manage/movie/status?movieId=" + d[0]);
            notificationService.notifyManagersOfMovie((Integer) d[0], Notification.TYPE_MOVIE_STATUS,
                    "Phim \"" + d[1] + "\" tạm chuyển về Sắp chiếu",
                    "Hệ thống tự chuyển vì " + d[2] + ". Khách tạm thời không thấy phim ở mục Đang chiếu.",
                    "/movies/detail?id=" + d[0]);
        }
        return checked;
    }

    /** Dữ liệu phim đang chiếu còn thiếu (chi nhánh đang chiếu / thể loại / poster); rỗng = đủ. Không xét ngày khởi chiếu. */
    private List<String> missingShowingData(Connection con, Movie m, LocalDate today) throws SQLException {
        int movieId = m.getMovieId();
        List<String> missing = new ArrayList<>();
        if (movieBranchDAO.countScreeningByMovie(con, movieId, today) == 0) missing.add("không có chi nhánh đang chiếu");
        if (movieGenreDAO.countByMovie(con, movieId) == 0) missing.add("không có thể loại");
        if (movieMediaDAO.countByMovieAndType(con, movieId, MovieMedia.TYPE_POSTER) == 0) missing.add("không có poster");
        return missing;
    }

    private MovieStatusDTO load(Connection con, int movieId) throws SQLException {
        Movie m = movieDAO.findById(con, movieId);
        if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
        return new MovieStatusDTO(m,
                new ArrayList<>(TRANSITIONS.getOrDefault(m.getStatus(), List.of())),
                movieGenreDAO.countByMovie(con, movieId),
                movieBranchDAO.countScreeningByMovie(con, movieId, LocalDate.now()),
                movieMediaDAO.countByMovieAndType(con, movieId, MovieMedia.TYPE_POSTER),
                showtimeDAO.countUpcomingByMovie(con, movieId),
                m.getReleaseDate() != null && !m.getReleaseDate().isAfter(LocalDate.now()));
    }
}
