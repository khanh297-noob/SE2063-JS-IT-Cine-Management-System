package service.movie;

import dao.movie.GenreDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import dao.movie.MovieGenreDAO;
import dao.movie.MovieMediaDAO;
import dao.movie.MovieReviewDAO;
import model.movie.CustomerMovieDetailDTO;
import model.movie.Genre;
import model.movie.Movie;
import model.movie.MovieMedia;
import model.movie.MovieReview;
import model.movie.MovieSummaryDTO;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Giao diện khách hàng.
 *
 * BR-CU-01  Hiển thị tất cả phim đang chiếu ("Now Showing"). Trang vào tự do, không cần đăng nhập.
 * BR-CU-02  Lọc theo thể loại, năm phát hành (năm của ngày khởi chiếu), ngôn ngữ.
 * BR-CU-03  Đề cử tháng: tối đa 10 phim có lượt yêu thích cao nhất (BR-CU-06).
 * BR-CU-04  Đánh giá: tên người đánh giá bắt buộc (tối đa 50 ký tự), rating 1-5 sao, nội dung bắt buộc, tối đa 1000 ký tự.
 * BR-CU-05  Hiển thị thêm phim sắp chiếu ("Coming Soon"), xem được chi tiết nhưng chưa được đánh giá.
 * BR-CU-06  Lượt yêu thích lưu trong MovieReviews (liked = 1, không có rating / content). Mỗi người xem
 *           1 lượt / phim trong một phiên (controller nhớ trong HttpSession); tổng lượt = số dòng liked = 1.
 */
public class CustomerMovieService {

    public static final int RECOMMEND_LIMIT = 10;

    private final MovieDAO movieDAO = new MovieDAO();
    private final GenreDAO genreDAO = new GenreDAO();
    private final MovieGenreDAO movieGenreDAO = new MovieGenreDAO();
    private final MovieMediaDAO movieMediaDAO = new MovieMediaDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final MovieReviewDAO reviewDAO = new MovieReviewDAO();

    /** Các lựa chọn trên header: chỉ gồm giá trị có phim đang chiếu / sắp chiếu, để lọc không ra trang trống. */
    public static final class Menu {
        private final List<Genre> genres;
        private final List<Integer> years;
        private final List<String> languages;

        Menu(List<Genre> genres, List<Integer> years, List<String> languages) {
            this.genres = genres;
            this.years = years;
            this.languages = languages;
        }

        public List<Genre> getGenres() { return genres; }
        public List<Integer> getYears() { return years; }
        public List<String> getLanguages() { return languages; }
    }

    /** Phim đang chiếu, lọc theo các tham số khác null. */
    public List<MovieSummaryDTO> listShowing(Integer genreId, Integer year, String language)
            throws SQLException, BusinessException {
        return JdbcContext.query(con -> filter(load(con, Movie.STATUS_NOW_SHOWING, genreId), year, language));
    }

    /** BR-CU-05: phim sắp chiếu, ngày khởi chiếu gần nhất trước. */
    public List<MovieSummaryDTO> listComingSoon(Integer genreId, Integer year, String language)
            throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            List<MovieSummaryDTO> list = filter(load(con, Movie.STATUS_COMING_SOON, genreId), year, language);
            // BR-MS-03: phim đã quá ngày khởi chiếu mà vẫn "Coming Soon" (chưa đủ điều kiện chiếu) không hiện cho khách
            LocalDate today = LocalDate.now();
            list.removeIf(ms -> ms.getMovie().getReleaseDate() == null || !ms.getMovie().getReleaseDate().isAfter(today));
            list.sort(Comparator.comparing((MovieSummaryDTO ms) -> ms.getMovie().getReleaseDate(),
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return list;
        });
    }

    public Menu getMenu() throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            List<MovieSummaryDTO> visible = load(con, Movie.STATUS_NOW_SHOWING, null);
            visible.addAll(load(con, Movie.STATUS_COMING_SOON, null));
            Set<String> genreNames = new HashSet<>();
            Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());
            Set<String> languageKeys = new HashSet<>();
            List<String> languages = new ArrayList<>();
            for (MovieSummaryDTO ms : visible) {
                genreNames.addAll(ms.getGenreNames());
                Movie m = ms.getMovie();
                if (m.getReleaseYear() != null) years.add(m.getReleaseYear());
                if (m.getLanguage() != null && languageKeys.add(m.getLanguage().toLowerCase(Locale.ROOT)))
                    languages.add(m.getLanguage());
            }
            List<Genre> genres = new ArrayList<>();
            for (Genre g : genreDAO.findAll(con)) {
                if (genreNames.contains(g.getName())) genres.add(g);
            }
            languages.sort(String.CASE_INSENSITIVE_ORDER);
            return new Menu(genres, new ArrayList<>(years), languages);
        });
    }

    public Genre getGenre(int genreId) throws SQLException, BusinessException {
        Genre g = JdbcContext.query(con -> genreDAO.findById(con, genreId));
        if (g == null) throw new NotFoundException("Không tìm thấy thể loại #" + genreId);
        return g;
    }

    /**
     * Đề cử tháng: sắp theo lượt yêu thích giảm dần, bằng nhau thì phim khởi chiếu gần nhất trước.
     * favoriteCounts: movie_id -> số lượt yêu thích (đọc từ HttpSession).
     */
    public List<MovieSummaryDTO> recommend(List<MovieSummaryDTO> showing, Map<Integer, Integer> favoriteCounts) {
        List<MovieSummaryDTO> sorted = new ArrayList<>(showing);
        sorted.sort(Comparator
                .comparingInt((MovieSummaryDTO ms) -> favoriteCounts.getOrDefault(ms.getMovie().getMovieId(), 0))
                .reversed()
                .thenComparing(ms -> ms.getMovie().getReleaseDate(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ms -> ms.getMovie().getTitle(), String.CASE_INSENSITIVE_ORDER));
        return sorted.size() > RECOMMEND_LIMIT ? new ArrayList<>(sorted.subList(0, RECOMMEND_LIMIT)) : sorted;
    }

    public CustomerMovieDetailDTO getDetail(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Movie m = movieDAO.findById(con, movieId);
            List<String> genres = movieGenreDAO.findGenreNamesPerMovie(con).get(movieId);
            if (m == null || !(m.isNowShowing() || m.isComingSoon()))
                throw new NotFoundException("Phim #" + movieId + " không có trong lịch chiếu.");

            String trailer = null;
            for (MovieMedia media : movieMediaDAO.findByMovie(con, movieId)) {
                if (!media.isPoster() && media.getYoutubeEmbedUrl() != null && (trailer == null || media.isPrimary()))
                    trailer = media.getYoutubeEmbedUrl();
            }
            MovieSummaryDTO summary = new MovieSummaryDTO(m, genres,
                    movieMediaDAO.findPrimaryPosterUrls(con).get(movieId),
                    movieBranchDAO.countBranchesPerMovie(con).getOrDefault(movieId, 0));
            return new CustomerMovieDetailDTO(summary, trailer, reviewDAO.findByMovie(con, movieId));
        });
    }

    public void addReview(int movieId, String reviewerName, Integer rating, String content)
            throws SQLException, BusinessException {
        String name = reviewerName == null ? null : reviewerName.trim().replaceAll("\\s+", " ");
        String text = content == null ? null : content.strip();
        List<String> errors = new ArrayList<>();
        if (rating == null || rating < MovieReview.MIN_RATING || rating > MovieReview.MAX_RATING)
            errors.add("Chọn số sao từ " + MovieReview.MIN_RATING + " đến " + MovieReview.MAX_RATING + ".");
        if (name == null || name.isEmpty()) errors.add("Nhập tên của bạn.");
        else if (name.length() > MovieReview.MAX_NAME) errors.add("Tên tối đa " + MovieReview.MAX_NAME + " ký tự.");
        if (text == null || text.isEmpty()) errors.add("Nhập nội dung đánh giá.");
        else if (text.length() > MovieReview.MAX_CONTENT)
            errors.add("Đánh giá tối đa " + MovieReview.MAX_CONTENT + " ký tự.");
        if (!errors.isEmpty()) throw new BusinessException(errors);

        JdbcContext.transaction(con -> {
            Movie m = movieDAO.findById(con, movieId);
            if (m == null) throw new NotFoundException("Phim #" + movieId + " không có trong lịch chiếu.");
            if (!m.isNowShowing()) // BR-CU-04: phim sắp chiếu chưa được đánh giá
                throw new BusinessException("Phim \"" + m.getTitle() + "\" chưa khởi chiếu nên chưa thể đánh giá.");
            MovieReview r = new MovieReview();
            r.setMovieId(movieId);
            r.setReviewerName(name);
            r.setRating(rating);
            r.setContent(text);
            return reviewDAO.insert(con, r);
        });
    }

    /** BR-CU-06: movie_id -> tổng lượt yêu thích (số dòng liked = 1 trong MovieReviews). */
    public Map<Integer, Integer> likeCounts() throws SQLException, BusinessException {
        return JdbcContext.query(reviewDAO::countLikesPerMovie);
    }

    /**
     * BR-CU-06: thêm một lượt yêu thích cho phim đang chiếu, lưu thành một dòng liked = 1 trong MovieReviews.
     * Trả về review_id của dòng đó để người xem bỏ thích được (controller giữ id trong HttpSession).
     */
    public int like(int movieId, String reviewerName) throws SQLException, BusinessException {
        String name = reviewerName == null || reviewerName.isBlank() ? "Khách" : reviewerName.trim();
        return JdbcContext.transaction(con -> {
            Movie m = movieDAO.findById(con, movieId);
            if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
            if (!m.isNowShowing()) throw new BusinessException("Chỉ yêu thích được phim đang chiếu.");
            return reviewDAO.insertLike(con, movieId, name);
        });
    }

    /** Bỏ lượt yêu thích (likeId là review_id trả về từ like). */
    public void unlike(int movieId, int likeId) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> {
            reviewDAO.deleteLike(con, likeId, movieId);
            return null;
        });
    }

    // ------------------------------------------------------------------

    private static List<MovieSummaryDTO> filter(List<MovieSummaryDTO> all, Integer year, String language) {
        List<MovieSummaryDTO> result = new ArrayList<>();
        for (MovieSummaryDTO ms : all) {
            Movie m = ms.getMovie();
            if (year != null && !year.equals(m.getReleaseYear())) continue;
            if (language != null && (m.getLanguage() == null || !m.getLanguage().equalsIgnoreCase(language))) continue;
            result.add(ms);
        }
        return result;
    }

    private List<MovieSummaryDTO> load(Connection con, String status, Integer genreId) throws SQLException {
        Map<Integer, List<String>> genres = movieGenreDAO.findGenreNamesPerMovie(con);
        Map<Integer, String> posters = movieMediaDAO.findPrimaryPosterUrls(con);
        Map<Integer, Integer> branches = movieBranchDAO.countBranchesPerMovie(con);
        List<MovieSummaryDTO> list = new ArrayList<>();
        for (Movie m : movieDAO.findAll(con, status, genreId)) {
            int id = m.getMovieId();
            list.add(new MovieSummaryDTO(m, genres.get(id), posters.get(id), branches.getOrDefault(id, 0)));
        }
        return list;
    }

}
