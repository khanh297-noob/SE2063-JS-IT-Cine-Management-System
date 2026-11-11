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
 *   End of Screening -> Now Showing (chiếu lại), Coming Soon (lên lịch chiếu lại / khôi phục phim huỷ nhầm)
 *
 * Ngày khởi chiếu (Movie.ReleaseDate) là lần đầu phim ra rạp, không đổi khi chiếu lại. Từng đợt chiếu tại rạp
 * do khung ngày chiếu của chi nhánh quyết định (MovieBranch.AvailableFrom / AvailableTo, màn Chi nhánh chiếu).
 *
 * BR-MS-01  "Now Showing" khi: đã tới ngày khởi chiếu, có ít nhất 1 thể loại, 1 poster và 1 chi nhánh
 *           đang chiếu (chi nhánh đang hoạt động và hôm nay nằm trong khung ngày chiếu của chi nhánh đó).
 *           Phim không có thể loại (vd thể loại duy nhất bị xoá) không được phát sóng.
 * BR-MS-02  Sang "End of Screening": không còn suất chiếu sắp tới. Đổi trạng thái khoá dòng phim và các suất chiếu
 *           của phim tới hết giao dịch, để không ai xếp thêm suất chiếu / đổi trạng thái cùng lúc.
 * BR-MS-03  Đối soát tự động (khi khởi động, mỗi ngày lúc 00:01, mỗi lần mở trang quản lý phim / thể loại,
 *           sau khi đổi trạng thái chi nhánh), với phim "Now Showing" không còn chi nhánh đang chiếu / thể loại /
 *           poster và không còn suất chiếu sắp tới:
 *           - mọi khung ngày chiếu đều đã kết thúc (đợt chiếu đã xong ở mọi rạp) -> "End of Screening";
 *           - còn lại (chi nhánh bị ngưng / gỡ, thiếu thể loại, thiếu poster) -> "Coming Soon", giống BR-GE-02.
 *           Còn suất chiếu sắp tới thì không tự chuyển, chỉ báo Admin (tối đa 1 lần / ngày).
 *           Ngày khởi chiếu KHÔNG làm trạng thái tự đổi.
 * BR-MS-04  Phim "Coming Soon" đã đủ điều kiện BR-MS-01 (vd khung ngày chiếu ở chi nhánh vừa bắt đầu):
 *           không tự chuyển, nhắc Admin chuyển sang "Now Showing" (1 lần cho mỗi đợt chiếu).
 * Thông báo (gửi sau khi đã commit, lỗi gửi không ảnh hưởng đổi trạng thái):
 *   - Phim sang "Now Showing" -> mọi khách hàng ("Phim mới đang chiếu" / "Phim chiếu lại").
 *   - Kết quả đối soát (BR-MS-03, BR-MS-04) -> mọi Admin.
 *   - Mọi lần đổi trạng thái (tay hoặc đối soát) -> Quản lý các chi nhánh đang gán phim đó.
 */
public class MovieStatusService {

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            Movie.STATUS_COMING_SOON, List.of(Movie.STATUS_NOW_SHOWING, Movie.STATUS_ENDED),
            Movie.STATUS_NOW_SHOWING, List.of(Movie.STATUS_ENDED),
            Movie.STATUS_ENDED, List.of(Movie.STATUS_NOW_SHOWING, Movie.STATUS_COMING_SOON));

    private final MovieDAO movieDAO = new MovieDAO();
    private final MovieGenreDAO movieGenreDAO = new MovieGenreDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final MovieMediaDAO movieMediaDAO = new MovieMediaDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final NotificationService notificationService = new NotificationService();

    public MovieStatusDTO getStatus(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> load(con, movieDAO.findById(con, movieId), movieId,
                showtimeDAO.countUpcomingByMovie(con, movieId)));
    }

    public void changeStatus(int movieId, String target) throws SQLException, BusinessException {
        changeStatus(movieId, null, target);
    }

    /**
     * expectedFrom: trạng thái người dùng nhìn thấy khi bấm (form gửi kèm); khác trạng thái hiện tại nghĩa là
     * phim vừa bị người khác đổi, từ chối để không đổi tiếp dựa trên màn hình cũ. null = không kiểm tra.
     */
    public void changeStatus(int movieId, String expectedFrom, String target) throws SQLException, BusinessException {
        Movie before = JdbcContext.transaction(con -> {
            // BR-MS-02: khoá phim trước khi kiểm tra, người thao tác sau phải chờ rồi kiểm tra trên trạng thái mới
            Movie locked = movieDAO.findByIdForUpdate(con, movieId);
            MovieStatusDTO s = load(con, locked, movieId, movieDAO.countUpcomingShowtimesLocked(con, movieId));
            Movie m = s.getMovie();
            if (expectedFrom != null && !expectedFrom.equals(m.getStatus()))
                throw new BusinessException("Phim \"" + m.getTitle() + "\" vừa được người khác chuyển sang "
                        + statusLabel(m.getStatus()) + ". Kiểm tra lại trạng thái hiện tại trước khi đổi.");
            if (target == null || !s.getAllowedTargets().contains(target))
                throw new BusinessException("Không thể chuyển \"" + m.getTitle() + "\" từ "
                        + statusLabel(m.getStatus()) + " sang " + statusLabel(target) + ".");

            List<String> errors = new ArrayList<>();
            if (Movie.STATUS_NOW_SHOWING.equals(target)) {
                if (!s.isReleased()) errors.add("Chưa tới ngày khởi chiếu " + m.getReleaseDateText() + ".");
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

    /** Tên trạng thái tiếng Việt hiển thị cho người dùng. */
    public static String statusLabel(String status) {
        if (Movie.STATUS_NOW_SHOWING.equals(status)) return "Đang chiếu";
        if (Movie.STATUS_COMING_SOON.equals(status)) return "Sắp chiếu";
        if (Movie.STATUS_ENDED.equals(status)) return "Ngưng chiếu";
        return status;
    }

    /** Kết quả đối soát BR-MS-03 / BR-MS-04, để báo lại cho Admin / ghi log. */
    public static final class RuleCheckResult {
        private final List<String> demoted = new ArrayList<>();
        private final List<String> ended = new ArrayList<>();
        private final List<String> needsAttention = new ArrayList<>();
        private final List<String> readyToShow = new ArrayList<>();

        /** Phim đã chuyển về "Coming Soon", kèm lý do. */
        public List<String> getDemoted() { return demoted; }
        /** Phim đã chuyển sang "End of Screening" vì mọi khung ngày chiếu đã kết thúc. */
        public List<String> getEnded() { return ended; }
        /** Phim vi phạm nhưng còn suất chiếu sắp tới nên không tự chuyển. */
        public List<String> getNeedsAttention() { return needsAttention; }
        /** Phim "Coming Soon" đã đủ điều kiện chuyển sang "Now Showing" (chỉ nhắc, không tự chuyển). */
        public List<String> getReadyToShow() { return readyToShow; }
        /** true nếu lần đối soát này không đổi dữ liệu và không có gì cần Admin xử lý gấp. */
        public boolean isClean() { return demoted.isEmpty() && ended.isEmpty() && needsAttention.isEmpty(); }

        /** Thông báo cho Admin về các thay đổi / việc cần xử lý; null nếu isClean(). */
        public String toMessage() {
            if (isClean()) return null;
            StringBuilder msg = new StringBuilder();
            if (!demoted.isEmpty())
                msg.append("Đã chuyển ").append(demoted.size())
                   .append(" phim từ Đang chiếu về Sắp chiếu vì không còn đủ điều kiện đang chiếu: ")
                   .append(String.join("; ", demoted)).append(". ");
            if (!ended.isEmpty())
                msg.append("Đã chuyển ").append(ended.size())
                   .append(" phim sang Ngưng chiếu vì mọi khung ngày chiếu đã kết thúc: ")
                   .append(String.join("; ", ended)).append(". ");
            if (!needsAttention.isEmpty())
                msg.append("Cần xử lý: ").append(String.join("; ", needsAttention))
                   .append(". Phim đang chiếu nhưng thiếu điều kiện, chưa tự chuyển vì còn suất chiếu.");
            return msg.toString().trim();
        }
    }

    /** BR-MS-03 và BR-MS-04. */
    public RuleCheckResult enforceStatusRules() throws SQLException, BusinessException {
        LocalDate today = LocalDate.now();
        List<Object[]> changes = new ArrayList<>();   // {movieId, tên, trạng thái mới, lý do}
        List<Object[]> attention = new ArrayList<>(); // {movieId, tên, lý do}
        List<Object[]> ready = new ArrayList<>();     // {movieId, tên, ngày bắt đầu đợt chiếu}
        RuleCheckResult checked = JdbcContext.transaction(con -> {
            RuleCheckResult result = new RuleCheckResult();

            for (Movie m : movieDAO.findAll(con, Movie.STATUS_NOW_SHOWING, null)) {
                List<String> missing = missingShowingData(con, m, today);
                if (missing.isEmpty()) continue;
                int movieId = m.getMovieId();
                String why = String.join(", ", missing);
                int upcoming = showtimeDAO.countUpcomingByMovie(con, movieId);
                if (upcoming > 0) {
                    result.needsAttention.add("\"" + m.getTitle() + "\" (" + why + ", còn " + upcoming + " suất chiếu sắp tới)");
                    attention.add(new Object[]{movieId, m.getTitle(), why + ", còn " + upcoming + " suất chiếu sắp tới"});
                    continue;
                }
                boolean windowsOver = movieBranchDAO.allWindowsEnded(con, movieId, today);
                String target = windowsOver ? Movie.STATUS_ENDED : Movie.STATUS_COMING_SOON;
                if (windowsOver) why = "mọi khung ngày chiếu tại chi nhánh đã kết thúc";
                // Admin vừa đổi trạng thái phim này thì bỏ qua, lần đối soát sau xét lại
                if (!movieDAO.updateStatusIf(con, movieId, Movie.STATUS_NOW_SHOWING, target)) continue;
                (windowsOver ? result.ended : result.demoted).add("\"" + m.getTitle() + "\" (" + why + ")");
                changes.add(new Object[]{movieId, m.getTitle(), target, why});
            }

            for (Movie m : movieDAO.findAll(con, Movie.STATUS_COMING_SOON, null)) {
                if (m.getReleaseDate() == null || m.getReleaseDate().isAfter(today)) continue;
                if (!missingShowingData(con, m, today).isEmpty()) continue;
                LocalDate since = movieBranchDAO.findScreeningSince(con, m.getMovieId(), today);
                result.readyToShow.add("\"" + m.getTitle() + "\"");
                ready.add(new Object[]{m.getMovieId(), m.getTitle(), since == null ? today : since});
            }
            return result;
        });

        // Gửi sau khi commit. Đổi trạng thái chỉ xảy ra một lần nên thông báo một lần.
        for (Object[] c : changes) {
            boolean toEnded = Movie.STATUS_ENDED.equals(c[2]);
            notificationService.notifyAdmins(Notification.TYPE_MOVIE_STATUS,
                    (toEnded ? "Phim đã chiếu xong, chuyển sang Ngưng chiếu: " : "Phim bị chuyển về Sắp chiếu: ") + c[1],
                    "Lý do: " + c[3] + "." + (toEnded
                            ? " Muốn chiếu lại: chuyển phim về Sắp chiếu rồi đặt khung ngày chiếu mới cho chi nhánh."
                            : " Bổ sung dữ liệu rồi chuyển lại sang Đang chiếu."),
                    "/manage/movie/status?movieId=" + c[0]);
            notificationService.notifyManagersOfMovie((Integer) c[0], Notification.TYPE_MOVIE_STATUS,
                    toEnded ? "Phim \"" + c[1] + "\" đã chuyển sang Ngưng chiếu"
                            : "Phim \"" + c[1] + "\" tạm chuyển về Sắp chiếu",
                    toEnded ? "Khung ngày chiếu tại các chi nhánh đã kết thúc. Khách không còn thấy phim ở mục Đang chiếu."
                            : "Hệ thống tự chuyển vì " + c[3] + ". Khách tạm thời không thấy phim ở mục Đang chiếu.",
                    "/movies/detail?id=" + c[0]);
        }
        // Còn suất chiếu nên không tự chuyển: nhắc tối đa 1 lần / ngày tới khi Admin xử lý
        for (Object[] a : attention) {
            notificationService.notifyAdminsOnce(Notification.TYPE_MOVIE_STATUS,
                    "Phim đang chiếu thiếu điều kiện: " + a[1],
                    "Lý do: " + a[2] + ". Hệ thống chưa tự chuyển trạng thái vì còn suất chiếu.",
                    "/manage/movie/status?movieId=" + a[0], today.atStartOfDay());
        }
        // BR-MS-04: 1 lần cho mỗi đợt chiếu (tính từ ngày bắt đầu khung chiếu đang hiệu lực)
        for (Object[] r : ready) {
            notificationService.notifyAdminsOnce(Notification.TYPE_MOVIE_STATUS,
                    "Phim có thể bắt đầu chiếu: " + r[1],
                    "Phim đủ điều kiện (đã tới ngày khởi chiếu, có thể loại, poster và chi nhánh đang trong khung ngày chiếu) "
                            + "nhưng vẫn đang ở Sắp chiếu. Chuyển sang Đang chiếu để khách đặt vé.",
                    "/manage/movie/status?movieId=" + r[0], ((LocalDate) r[2]).atStartOfDay());
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

    private MovieStatusDTO load(Connection con, Movie m, int movieId, int upcomingShowtimes) throws SQLException {
        if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
        return new MovieStatusDTO(m,
                new ArrayList<>(TRANSITIONS.getOrDefault(m.getStatus(), List.of())),
                movieGenreDAO.countByMovie(con, movieId),
                movieBranchDAO.countScreeningByMovie(con, movieId, LocalDate.now()),
                movieMediaDAO.countByMovieAndType(con, movieId, MovieMedia.TYPE_POSTER),
                upcomingShowtimes,
                m.getReleaseDate() != null && !m.getReleaseDate().isAfter(LocalDate.now()));
    }
}
