package cinemanagement.service;

import cinemanagement.dao.GenreDAO;
import cinemanagement.dao.MovieDAO;
import cinemanagement.dao.MovieGenreDAO;
import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.util.BusinessException;
import cinemanagement.util.NotFoundException;
import cinemanagement.model.Genre;
import cinemanagement.model.Movie;
import cinemanagement.util.DBContext;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manage Genre.
 *
 * BR-GE-01  Tên thể loại bắt buộc, tối đa 50 ký tự, không trùng (không phân biệt hoa thường).
 * BR-GE-02  Xoá thể loại: gỡ khỏi các phim trước (xem delete).
 */
public class GenreService {

    private final GenreDAO genreDAO = new GenreDAO();
    private final MovieGenreDAO movieGenreDAO = new MovieGenreDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    public List<Genre> list() throws SQLException, BusinessException {
        return DBContext.query(genreDAO::findAll);
    }

    /** genre_id -> số phim. */
    public Map<Integer, Integer> countMoviesPerGenre() throws SQLException, BusinessException {
        return DBContext.query(movieGenreDAO::countMoviesPerGenre);
    }

    public Genre get(int genreId) throws SQLException, BusinessException {
        Genre g = DBContext.query(con -> genreDAO.findById(con, genreId));
        if (g == null) throw new NotFoundException("Không tìm thấy thể loại #" + genreId);
        return g;
    }

    public int save(Genre g) throws SQLException, BusinessException {
        g.setName(g.getName() == null ? null : g.getName().trim().replaceAll("\\s+", " "));
        if (g.getName() != null && g.getName().isEmpty()) g.setName(null);
        if (g.getDescription() != null && g.getDescription().isBlank()) g.setDescription(null);

        return DBContext.transaction(con -> {
            List<String> errors = new ArrayList<>();
            if (g.getName() == null) errors.add("Nhập tên thể loại.");
            else if (g.getName().length() > 50) errors.add("Tên thể loại tối đa 50 ký tự.");
            else {
                Genre dup = findSameName(genreDAO.findAll(con), g.getName(), g.getGenreId());
                if (dup != null)
                    errors.add("Thể loại \"" + dup.getName() + "\" đã tồn tại (không phân biệt hoa thường). "
                            + "Vui lòng nhập tên thể loại khác.");
            }
            if (g.getDescription() != null && g.getDescription().length() > 255)
                errors.add("Mô tả tối đa 255 ký tự.");
            if (!errors.isEmpty()) throw new BusinessException(errors);

            if (g.getGenreId() == 0) return genreDAO.insert(con, g);
            if (genreDAO.findById(con, g.getGenreId()) == null)
                throw new NotFoundException("Không tìm thấy thể loại #" + g.getGenreId());
            genreDAO.update(con, g);
            return g.getGenreId();
        });
    }

    /**
     * BR-GE-01: thể loại khác (khác excludeId) có cùng tên, so sánh không phân biệt hoa thường
     * nhưng vẫn phân biệt dấu ("hài" trùng "Hài", "Hai" không trùng "Hài").
     * So sánh ở Java thay vì SQL để không phụ thuộc collation của DB.
     */
    static Genre findSameName(List<Genre> genres, String name, int excludeId) {
        String key = nameKey(name);
        for (Genre other : genres) {
            if (other.getGenreId() != excludeId && nameKey(other.getName()).equals(key)) return other;
        }
        return null;
    }

    /** Chuẩn NFC (gõ dấu tổ hợp hay dựng sẵn đều như nhau), gộp khoảng trắng, viết thường. */
    private static String nameKey(String name) {
        if (name == null) return "";
        return Normalizer.normalize(name, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Kết quả xoá thể loại, để báo lại cho người dùng. */
    public static final class DeleteResult {
        private final String genreName;
        private final int detached;
        private final List<String> leftWithoutGenre;
        private final List<String> stoppedShowing;

        DeleteResult(String genreName, int detached, List<String> leftWithoutGenre, List<String> stoppedShowing) {
            this.genreName = genreName;
            this.detached = detached;
            this.leftWithoutGenre = leftWithoutGenre;
            this.stoppedShowing = stoppedShowing;
        }

        public String getGenreName() { return genreName; }
        /** Số phim bị gỡ thể loại này. */
        public int getDetached() { return detached; }
        /** Tên các phim không còn thể loại nào. */
        public List<String> getLeftWithoutGenre() { return leftWithoutGenre; }
        /** Tên các phim đang chiếu bị chuyển về "Coming Soon" vì không còn thể loại. */
        public List<String> getStoppedShowing() { return stoppedShowing; }
    }

    /**
     * BR-GE-02  Xoá thể loại: gỡ thể loại khỏi mọi phim rồi mới xoá.
     *           Phim chỉ có đúng thể loại này sẽ không còn thể loại nào (thể loại = null) và không được
     *           phát sóng (BR-MS-01): phim đang chiếu bị chuyển về "Coming Soon".
     *           Không xoá được nếu một phim như vậy còn suất chiếu sắp tới (tránh dừng chiếu khi đã bán vé).
     */
    public DeleteResult delete(int genreId) throws SQLException, BusinessException {
        return DBContext.transaction(con -> {
            Genre g = genreDAO.findById(con, genreId);
            if (g == null) throw new NotFoundException("Không tìm thấy thể loại #" + genreId);

            List<Movie> orphans = new ArrayList<>();
            for (int movieId : movieGenreDAO.findMoviesWithOnlyGenre(con, genreId)) {
                Movie m = movieDAO.findById(con, movieId);
                if (m != null) orphans.add(m);
            }

            List<String> blocked = new ArrayList<>();
            for (Movie m : orphans) {
                if (!m.isNowShowing()) continue;
                int upcoming = showtimeDAO.countUpcomingByMovie(con, m.getMovieId());
                if (upcoming > 0) blocked.add("\"" + m.getTitle() + "\" (" + upcoming + " suất)");
            }
            if (!blocked.isEmpty())
                throw new BusinessException("Không xoá được thể loại \"" + g.getName() + "\": các phim "
                        + String.join(", ", blocked) + " chỉ có thể loại này, đang chiếu và còn suất chiếu sắp tới. "
                        + "Thêm thể loại khác cho các phim này hoặc huỷ suất chiếu trước.");

            int detached = movieGenreDAO.deleteByGenre(con, genreId);
            List<String> leftWithoutGenre = new ArrayList<>();
            List<String> stoppedShowing = new ArrayList<>();
            for (Movie m : orphans) {
                leftWithoutGenre.add(m.getTitle());
                if (m.isNowShowing()) {
                    movieDAO.updateStatus(con, m.getMovieId(), Movie.STATUS_COMING_SOON);
                    stoppedShowing.add(m.getTitle());
                }
            }
            genreDAO.delete(con, genreId);
            return new DeleteResult(g.getName(), detached, leftWithoutGenre, stoppedShowing);
        });
    }
}
