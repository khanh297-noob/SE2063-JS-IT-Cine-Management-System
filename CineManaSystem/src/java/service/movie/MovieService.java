package service.movie;

import dao.movie.ShowtimeDAO;
import dao.movie.GenreDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import dao.movie.MovieGenreDAO;
import dao.movie.MovieMediaDAO;
import model.movie.Genre;
import model.movie.Movie;
import model.movie.MovieSummaryDTO;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;
import util.TextUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manage Movie.
 *
 * BR-MV-01  Tên phim bắt buộc (tối đa 200 ký tự); thời lượng 1-600 phút; ngày khởi chiếu bắt buộc.
 * BR-MV-02  Phân loại tuổi thuộc P, K, T13, T16, T18.
 * BR-MV-03  Phim phải có ít nhất một thể loại.
 * BR-MV-04  Phim mới luôn ở trạng thái "Coming Soon"; đổi trạng thái qua màn Movie Status.
 * BR-MV-05  Không xoá phim đã có suất chiếu; hãy chuyển "End of Screening".
 */
public class MovieService {

    /** Độ dài tối đa của một tên diễn viên. */
    public static final int MAX_ACTOR_NAME = 100;

    private final MovieDAO movieDAO = new MovieDAO();
    private final GenreDAO genreDAO = new GenreDAO();
    private final MovieGenreDAO movieGenreDAO = new MovieGenreDAO();
    private final MovieMediaDAO movieMediaDAO = new MovieMediaDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    public List<MovieSummaryDTO> list(String keyword, String status, Integer genreId)
            throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Map<Integer, List<String>> genres = movieGenreDAO.findGenreNamesPerMovie(con);
            Map<Integer, String> posters = movieMediaDAO.findPrimaryPosterUrls(con);
            Map<Integer, Integer> branches = movieBranchDAO.countBranchesPerMovie(con);
            List<MovieSummaryDTO> list = new ArrayList<>();
            for (Movie m : movieDAO.findAll(con, status, genreId)) {
                if (!TextUtil.matchesAny(keyword, m.getTitle())) continue;
                int id = m.getMovieId();
                list.add(new MovieSummaryDTO(m, genres.get(id), posters.get(id), branches.getOrDefault(id, 0)));
            }
            return list;
        });
    }

    public Movie get(int movieId) throws SQLException, BusinessException {
        Movie m = JdbcContext.query(con -> movieDAO.findById(con, movieId));
        if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
        return m;
    }

    /** Phim + tên thể loại, poster chính, số chi nhánh: cho trang Chi tiết phim. */
    public MovieSummaryDTO getSummary(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Movie m = movieDAO.findById(con, movieId);
            if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
            return new MovieSummaryDTO(m,
                    movieGenreDAO.findGenreNamesPerMovie(con).get(movieId),
                    movieMediaDAO.findPrimaryPosterUrls(con).get(movieId),
                    movieBranchDAO.countBranchesPerMovie(con).getOrDefault(movieId, 0));
        });
    }

    public Set<Integer> getGenreIds(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> movieGenreDAO.findGenreIdsByMovie(con, movieId));
    }

    public int save(Movie m, Set<Integer> genreIds) throws SQLException, BusinessException {
        m.setTitle(clean(m.getTitle()));
        m.setDirector(clean(m.getDirector()));
        m.setCastMembers(cleanCast(m.getCastMembers()));
        m.setLanguage(clean(m.getLanguage()));
        if (m.getDescription() != null && m.getDescription().isBlank()) m.setDescription(null);

        return JdbcContext.transaction(con -> {
            List<String> errors = new ArrayList<>();
            if (m.getTitle() == null) errors.add("Nhập tên phim.");
            else if (m.getTitle().length() > 200) errors.add("Tên phim tối đa 200 ký tự.");
            if (m.getDirector() != null && m.getDirector().length() > 150) errors.add("Đạo diễn tối đa 150 ký tự.");
            if (m.getCastMembers() != null && m.getCastMembers().length() > 500)
                errors.add("Danh sách diễn viên quá dài (tối đa 500 ký tự). Bớt một vài diễn viên.");
            for (String actor : m.getCastList()) {
                if (actor.length() > MAX_ACTOR_NAME) {
                    errors.add("Tên diễn viên \"" + actor + "\" dài quá " + MAX_ACTOR_NAME + " ký tự.");
                    break;
                }
            }
            if (m.getLanguage() != null && m.getLanguage().length() > 50) errors.add("Ngôn ngữ tối đa 50 ký tự.");
            if (m.getDuration() == null) errors.add("Nhập thời lượng (phút).");
            else if (m.getDuration() < 1 || m.getDuration() > 600) errors.add("Thời lượng từ 1 đến 600 phút.");
            if (m.getReleaseDate() == null) errors.add("Chọn ngày khởi chiếu.");
            if (m.getAgeRating() == null || !Movie.AGE_RATINGS.contains(m.getAgeRating()))
                errors.add("Chọn phân loại tuổi.");

            Set<Integer> validGenres = new HashSet<>();
            for (Genre g : genreDAO.findAll(con)) validGenres.add(g.getGenreId());
            if (genreIds == null || genreIds.isEmpty()) errors.add("Chọn ít nhất một thể loại.");
            else if (!validGenres.containsAll(genreIds)) errors.add("Có thể loại không tồn tại. Tải lại trang.");
            if (!errors.isEmpty()) throw new BusinessException(errors);

            int id;
            if (m.getMovieId() == 0) {
                m.setStatus(Movie.STATUS_COMING_SOON);
                id = movieDAO.insert(con, m);
            } else {
                if (movieDAO.findById(con, m.getMovieId()) == null)
                    throw new NotFoundException("Không tìm thấy phim #" + m.getMovieId());
                movieDAO.update(con, m);
                id = m.getMovieId();
            }
            movieGenreDAO.replaceForMovie(con, id, genreIds);
            return id;
        });
    }

    public void delete(int movieId) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> {
            Movie m = movieDAO.findById(con, movieId);
            if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
            if (showtimeDAO.countByMovie(con, movieId) > 0)
                throw new BusinessException("\"" + m.getTitle() + "\" đã có suất chiếu nên không xoá được. "
                        + "Hãy chuyển trạng thái sang Ngưng chiếu.");
            movieDAO.delete(con, movieId); // MovieGenres, MovieMedia, MovieBranches xoá theo ON DELETE CASCADE
            return null;
        });
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /**
     * Chuẩn hoá danh sách diễn viên "A, B, C": tách theo dấu phẩy, gộp khoảng trắng,
     * bỏ tên rỗng và tên trùng (không phân biệt hoa thường), nối lại bằng ", ".
     */
    static String cleanCast(String raw) {
        if (raw == null) return null;
        Map<String, String> unique = new LinkedHashMap<>();
        for (String part : raw.split(",")) {
            String name = clean(part);
            if (name != null) unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        return unique.isEmpty() ? null : String.join(", ", unique.values());
    }
}
