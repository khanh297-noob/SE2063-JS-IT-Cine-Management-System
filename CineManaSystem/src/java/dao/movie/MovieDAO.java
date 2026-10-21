package dao.movie;

import model.movie.Movie;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Bảng Movie. */
public class MovieDAO {

    private static final String COLUMNS = "m.MovieID, m.title, m.description, m.director, "
            + "m.CastMembers, m.language, m.duration, m.ReleaseDate, m.AgeRating, m.status, m.CreatedAt";

    private static final String ORDER = " ORDER BY CASE m.status WHEN 'Now Showing' THEN 0 "
            + "WHEN 'Coming Soon' THEN 1 ELSE 2 END, m.ReleaseDate DESC, m.title";

    /** Lọc theo trạng thái / thể loại. Tìm theo tên làm ở MovieService để không phân biệt dấu. */
    public List<Movie> findAll(Connection con, String status, Integer genreId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM Movie m WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append("AND m.status = ? ");
            params.add(status);
        }
        if (genreId != null) {
            sql.append("AND EXISTS (SELECT 1 FROM MovieGenre mg WHERE mg.MovieID = m.MovieID AND mg.GenreID = ?) ");
            params.add(genreId);
        }
        sql.append(ORDER);
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return mapList(ps);
        }
    }

    public Movie findById(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNS + " FROM Movie m WHERE m.MovieID = ?")) {
            ps.setInt(1, movieId);
            List<Movie> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /** Phim đang chiếu tại chi nhánh vào ngày cho trước (theo MovieBranch). */
    public List<Movie> findNowShowingAtBranch(Connection con, int branchId, LocalDate date) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM Movie m "
                + "JOIN MovieBranch mb ON mb.MovieID = m.MovieID AND mb.BranchID = ? "
                + "WHERE m.status = 'Now Showing' "
                + "AND (mb.AvailableFrom IS NULL OR mb.AvailableFrom <= ?) "
                + "AND (mb.AvailableTo IS NULL OR mb.AvailableTo >= ?)" + ORDER;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(date));
            ps.setDate(3, Date.valueOf(date));
            return mapList(ps);
        }
    }

    public int insert(Connection con, Movie m) throws SQLException {
        String sql = "INSERT INTO Movie (title, description, director, CastMembers, language, "
                + "duration, ReleaseDate, AgeRating, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEditable(ps, m);
            ps.setString(9, m.getStatus());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    /** Không sửa status ở đây: trạng thái đổi qua MovieStatusService. */
    public void update(Connection con, Movie m) throws SQLException {
        String sql = "UPDATE Movie SET title = ?, description = ?, director = ?, "
                + "CastMembers = ?, language = ?, duration = ?, ReleaseDate = ?, AgeRating = ? WHERE MovieID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindEditable(ps, m);
            ps.setInt(9, m.getMovieId());
            ps.executeUpdate();
        }
    }

    /**
     * Chép poster / trailer CHÍNH từ MovieMedia sang Movie.PosterURL / TrailerURL
     * (module đặt vé, báo cáo của nhóm đọc thẳng 2 cột này). Gọi sau mỗi lần đổi media chính.
     */
    public void syncPrimaryMedia(Connection con, int movieId) throws SQLException {
        String sql = "UPDATE Movie SET "
                + "PosterURL = (SELECT TOP 1 URL FROM MovieMedia WHERE MovieID = ? AND MediaType = 'POSTER' AND IsPrimary = 1), "
                + "TrailerURL = (SELECT TOP 1 URL FROM MovieMedia WHERE MovieID = ? AND MediaType = 'TRAILER' AND IsPrimary = 1) "
                + "WHERE MovieID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, movieId);
            ps.setInt(3, movieId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int movieId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Movie SET status = ? WHERE MovieID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, movieId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Movie WHERE MovieID = ?")) {
            ps.setInt(1, movieId);
            ps.executeUpdate();
        }
    }

    private void bindEditable(PreparedStatement ps, Movie m) throws SQLException {
        ps.setString(1, m.getTitle());
        JdbcUtil.setString(ps, 2, m.getDescription());
        JdbcUtil.setString(ps, 3, m.getDirector());
        JdbcUtil.setString(ps, 4, m.getCastMembers());
        JdbcUtil.setString(ps, 5, m.getLanguage());
        ps.setInt(6, m.getDuration());
        JdbcUtil.setDate(ps, 7, m.getReleaseDate());
        ps.setString(8, m.getAgeRating());
    }

    private List<Movie> mapList(PreparedStatement ps) throws SQLException {
        List<Movie> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Movie m = new Movie();
                m.setMovieId(rs.getInt("MovieID"));
                m.setTitle(rs.getString("title"));
                m.setDescription(rs.getString("description"));
                m.setDirector(rs.getString("director"));
                m.setCastMembers(rs.getString("CastMembers"));
                m.setLanguage(rs.getString("language"));
                m.setDuration(rs.getInt("duration"));
                m.setReleaseDate(JdbcUtil.getDate(rs, "ReleaseDate"));
                m.setAgeRating(rs.getString("AgeRating"));
                m.setStatus(rs.getString("status"));
                m.setCreatedAt(JdbcUtil.getDateTime(rs, "CreatedAt"));
                list.add(m);
            }
        }
        return list;
    }
}
