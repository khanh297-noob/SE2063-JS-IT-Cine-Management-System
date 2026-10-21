package cinemanagement.dao.movie;

import cinemanagement.model.movie.MovieMedia;
import cinemanagement.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bảng MovieMedia. */
public class MovieMediaDAO {

    private static final String SELECT =
            "SELECT MediaID, MovieID, MediaType, url, caption, IsPrimary, CreatedAt FROM MovieMedia ";

    public List<MovieMedia> findByMovie(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT
                + "WHERE MovieID = ? ORDER BY MediaType, IsPrimary DESC, CreatedAt DESC")) {
            ps.setInt(1, movieId);
            return mapList(ps);
        }
    }

    public MovieMedia findById(Connection con, int mediaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE MediaID = ?")) {
            ps.setInt(1, mediaId);
            List<MovieMedia> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /** media mới nhất cùng loại (trừ excludeId), dùng để chọn poster chính thay thế. */
    public MovieMedia findLatestOther(Connection con, int movieId, String type, int excludeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT
                + "WHERE MovieID = ? AND MediaType = ? AND MediaID <> ? ORDER BY CreatedAt DESC")) {
            ps.setInt(1, movieId);
            ps.setString(2, type);
            ps.setInt(3, excludeId);
            List<MovieMedia> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * MovieID -> url poster chính. Đọc Movie.PosterURL: cột này được đồng bộ với poster chính
     * trong MovieMedia (MovieDAO.syncPrimaryMedia), và module khác cũng có thể ghi thẳng vào đó.
     */
    public Map<Integer, String> findPrimaryPosterUrls(Connection con) throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT MovieID, PosterURL FROM Movie WHERE PosterURL IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getString(2));
        }
        return map;
    }

    public int countByMovieAndType(Connection con, int movieId, String type) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM MovieMedia WHERE MovieID = ? AND MediaType = ?")) {
            ps.setInt(1, movieId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int insert(Connection con, MovieMedia m) throws SQLException {
        String sql = "INSERT INTO MovieMedia (MovieID, MediaType, url, caption, IsPrimary) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getMovieId());
            ps.setString(2, m.getMediaType());
            ps.setString(3, m.getUrl());
            JdbcUtil.setString(ps, 4, m.getCaption());
            ps.setBoolean(5, m.isPrimary());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    public void clearPrimary(Connection con, int movieId, String type) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE MovieMedia SET IsPrimary = 0 WHERE MovieID = ? AND MediaType = ? AND IsPrimary = 1")) {
            ps.setInt(1, movieId);
            ps.setString(2, type);
            ps.executeUpdate();
        }
    }

    public void setPrimary(Connection con, int mediaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE MovieMedia SET IsPrimary = 1 WHERE MediaID = ?")) {
            ps.setInt(1, mediaId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int mediaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM MovieMedia WHERE MediaID = ?")) {
            ps.setInt(1, mediaId);
            ps.executeUpdate();
        }
    }

    private List<MovieMedia> mapList(PreparedStatement ps) throws SQLException {
        List<MovieMedia> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MovieMedia m = new MovieMedia();
                m.setMediaId(rs.getInt("MediaID"));
                m.setMovieId(rs.getInt("MovieID"));
                m.setMediaType(rs.getString("MediaType"));
                m.setUrl(rs.getString("url"));
                m.setCaption(rs.getString("caption"));
                m.setPrimary(rs.getBoolean("IsPrimary"));
                m.setCreatedAt(JdbcUtil.getDateTime(rs, "CreatedAt"));
                list.add(m);
            }
        }
        return list;
    }
}
