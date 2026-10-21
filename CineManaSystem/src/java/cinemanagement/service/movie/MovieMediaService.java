package cinemanagement.service.movie;

import cinemanagement.dao.movie.MovieDAO;
import cinemanagement.dao.movie.MovieMediaDAO;
import cinemanagement.model.movie.MovieMedia;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DBContext;
import cinemanagement.util.NotFoundException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Manage Poster/Trailer.
 *
 * BR-MD-01  URL bắt buộc, bắt đầu bằng http:// hoặc https://, tối đa 500 ký tự.
 * BR-MD-02  Mỗi phim có tối đa một poster chính và một trailer chính.
 *           Media đầu tiên của mỗi loại tự động thành media chính.
 * BR-MD-03  Xoá media chính thì media mới nhất cùng loại được chọn làm chính.
 * BR-MD-04  Poster / trailer chính luôn được chép sang Movie.PosterURL / TrailerURL (database chung của nhóm).
 */
public class MovieMediaService {

    private static final Pattern URL = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);

    private final MovieMediaDAO mediaDAO = new MovieMediaDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    public List<MovieMedia> list(int movieId) throws SQLException, BusinessException {
        return DBContext.query(con -> mediaDAO.findByMovie(con, movieId));
    }

    public void add(MovieMedia m) throws SQLException, BusinessException {
        if (m.getUrl() != null) m.setUrl(m.getUrl().trim());
        if (m.getCaption() != null && m.getCaption().isBlank()) m.setCaption(null);

        List<String> errors = new ArrayList<>();
        if (!MovieMedia.TYPE_POSTER.equals(m.getMediaType()) && !MovieMedia.TYPE_TRAILER.equals(m.getMediaType()))
            errors.add("Chọn loại: poster hoặc trailer.");
        if (m.getUrl() == null || m.getUrl().isEmpty()) errors.add("Nhập đường dẫn.");
        else if (m.getUrl().length() > 500) errors.add("Đường dẫn tối đa 500 ký tự.");
        else if (!URL.matcher(m.getUrl()).matches()) errors.add("Đường dẫn phải bắt đầu bằng http:// hoặc https://");
        if (m.getCaption() != null && m.getCaption().length() > 150) errors.add("Chú thích tối đa 150 ký tự.");
        if (!errors.isEmpty()) throw new BusinessException(errors);

        DBContext.transaction(con -> {
            if (movieDAO.findById(con, m.getMovieId()) == null)
                throw new NotFoundException("Không tìm thấy phim #" + m.getMovieId());
            boolean first = mediaDAO.countByMovieAndType(con, m.getMovieId(), m.getMediaType()) == 0;
            if (first) m.setPrimary(true);
            if (m.isPrimary()) mediaDAO.clearPrimary(con, m.getMovieId(), m.getMediaType());
            int id = mediaDAO.insert(con, m);
            movieDAO.syncPrimaryMedia(con, m.getMovieId());
            return id;
        });
    }

    public void setPrimary(int mediaId) throws SQLException, BusinessException {
        DBContext.transaction(con -> {
            MovieMedia m = mediaDAO.findById(con, mediaId);
            if (m == null) throw new NotFoundException("Không tìm thấy media #" + mediaId);
            mediaDAO.clearPrimary(con, m.getMovieId(), m.getMediaType());
            mediaDAO.setPrimary(con, mediaId);
            movieDAO.syncPrimaryMedia(con, m.getMovieId());
            return null;
        });
    }

    /** Trả về movieId để servlet quay về đúng trang. */
    public int delete(int mediaId) throws SQLException, BusinessException {
        return DBContext.transaction(con -> {
            MovieMedia m = mediaDAO.findById(con, mediaId);
            if (m == null) throw new NotFoundException("Không tìm thấy media #" + mediaId);
            mediaDAO.delete(con, mediaId);
            if (m.isPrimary()) {
                MovieMedia next = mediaDAO.findLatestOther(con, m.getMovieId(), m.getMediaType(), mediaId);
                if (next != null) mediaDAO.setPrimary(con, next.getMediaId());
            }
            movieDAO.syncPrimaryMedia(con, m.getMovieId());
            return m.getMovieId();
        });
    }
}
