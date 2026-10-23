package service.movie;

import dao.movie.ShowtimeDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import dao.movie.MovieGenreDAO;
import dao.movie.MovieMediaDAO;
import model.movie.Movie;
import model.movie.MovieMedia;
import model.movie.MovieStatusDTO;
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
 * BR-MS-01  Sang "Now Showing": đã tới ngày khởi chiếu, có ít nhất 1 thể loại, 1 chi nhánh và 1 poster.
 *           Phim không có thể loại (vd thể loại duy nhất bị xoá) không được phát sóng.
 * BR-MS-02  Sang "End of Screening": không còn suất chiếu sắp tới.
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

    public MovieStatusDTO getStatus(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> load(con, movieId));
    }

    public void changeStatus(int movieId, String target) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> {
            MovieStatusDTO s = load(con, movieId);
            Movie m = s.getMovie();
            if (target == null || !s.getAllowedTargets().contains(target))
                throw new BusinessException("Không thể chuyển \"" + m.getTitle() + "\" từ "
                        + m.getStatus() + " sang " + target + ".");

            List<String> errors = new ArrayList<>();
            if (Movie.STATUS_NOW_SHOWING.equals(target)) {
                if (!s.isReleased()) errors.add("Chưa tới ngày khởi chiếu " + m.getReleaseDate() + ".");
                if (s.getGenreCount() == 0) errors.add("Phim chưa có thể loại nên không được phát sóng. Thêm thể loại cho phim trước.");
                if (s.getBranchCount() == 0) errors.add("Chưa chọn chi nhánh chiếu phim.");
                if (s.getPosterCount() == 0) errors.add("Chưa có poster.");
            } else if (Movie.STATUS_ENDED.equals(target) && s.getUpcomingShowtimes() > 0) {
                errors.add("Còn " + s.getUpcomingShowtimes() + " suất chiếu sắp tới. Huỷ các suất chiếu trước.");
            }
            if (!errors.isEmpty()) throw new BusinessException(errors);

            movieDAO.updateStatus(con, movieId, target);
            return null;
        });
    }

    private MovieStatusDTO load(Connection con, int movieId) throws SQLException {
        Movie m = movieDAO.findById(con, movieId);
        if (m == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
        return new MovieStatusDTO(m,
                new ArrayList<>(TRANSITIONS.getOrDefault(m.getStatus(), List.of())),
                movieGenreDAO.countByMovie(con, movieId),
                movieBranchDAO.countByMovie(con, movieId),
                movieMediaDAO.countByMovieAndType(con, movieId, MovieMedia.TYPE_POSTER),
                showtimeDAO.countUpcomingByMovie(con, movieId),
                m.getReleaseDate() != null && !m.getReleaseDate().isAfter(LocalDate.now()));
    }
}
