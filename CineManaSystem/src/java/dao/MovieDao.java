package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Movie;

/**
 * Truy vấn phim cho trang chủ khách hàng.
 * Poster ưu tiên ảnh POSTER chính trong MovieMedia, không có thì dùng Movie.PosterURL.
 * Dùng xong phải close() để trả kết nối (try-with-resources).
 */
public class MovieDao extends DBContext implements AutoCloseable {

    public static final String STATUS_NOW_SHOWING = "Now Showing";
    public static final String STATUS_COMING_SOON = "Coming Soon";

    private static final String SELECT_MOVIE =
            "SELECT m.MovieID, m.Title, m.Duration, m.ReleaseDate, m.AgeRating, m.TrailerURL, m.Status, "
          + "       COALESCE((SELECT TOP 1 mm.URL FROM MovieMedia mm "
          + "                 WHERE mm.MovieID = m.MovieID AND mm.MediaType = 'POSTER' "
          + "                 ORDER BY mm.IsPrimary DESC, mm.MediaID), m.PosterURL) AS Poster, "
          + "       (SELECT STRING_AGG(g.Name, ', ') WITHIN GROUP (ORDER BY g.Name) "
          + "        FROM MovieGenre mg INNER JOIN Genre g ON g.GenreID = mg.GenreID "
          + "        WHERE mg.MovieID = m.MovieID) AS Genres "
          + "FROM Movie m ";

    /**
     * Danh sách phim theo trạng thái.
     * Phim đang chiếu: mới khởi chiếu trước. Phim sắp chiếu: sắp ra mắt trước.
     */
    public List<Movie> findByStatus(String status) {
        String sql = SELECT_MOVIE + "WHERE m.Status = ? "
                + (STATUS_COMING_SOON.equals(status)
                        ? "ORDER BY m.ReleaseDate ASC, m.Title"
                        : "ORDER BY m.ReleaseDate DESC, m.Title");

        List<Movie> movies = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapMovie(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return movies;
    }

    private Movie mapMovie(ResultSet rs) throws SQLException {
        Movie m = new Movie();
        m.setMovieID(rs.getInt("MovieID"));
        m.setTitle(rs.getString("Title"));
        m.setDuration(rs.getInt("Duration"));
        m.setReleaseDate(rs.getDate("ReleaseDate"));
        m.setAgeRating(rs.getString("AgeRating"));
        m.setTrailerURL(rs.getString("TrailerURL"));
        m.setPosterURL(rs.getString("Poster"));
        m.setStatus(rs.getString("Status"));
        m.setGenres(rs.getString("Genres"));
        return m;
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
