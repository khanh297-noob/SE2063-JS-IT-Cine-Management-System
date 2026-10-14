package cinemanagement.dao;

import cinemanagement.model.MovieMedia;
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
            "SELECT media_id, movie_id, media_type, url, caption, is_primary, created_at FROM MovieMedia ";

    public List<MovieMedia> findByMovie(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT
                + "WHERE movie_id = ? ORDER BY media_type, is_primary DESC, created_at DESC")) {
            ps.setInt(1, movieId);
            return mapList(ps);
        }
    }

    public MovieMedia findById(Connection con, int mediaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE media_id = ?")) {
            ps.setInt(1, mediaId);
            List<MovieMedia> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /** media mới nhất cùng loại (trừ excludeId), dùng để chọn poster chính thay thế. */
    public MovieMedia findLatestOther(Connection con, int movieId, String type, int excludeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT
                + "WHERE movie_id = ? AND media_type = ? AND media_id <> ? ORDER BY created_at DESC")) {
            ps.setInt(1, movieId);
            ps.setString(2, type);
            ps.setInt(3, excludeId);
            List<MovieMedia> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /** movie_id -> url poster chính. */
    public Map<Integer, String> findPrimaryPosterUrls(Connection con) throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT movie_id, url FROM MovieMedia WHERE media_type = 'POSTER' AND is_primary = 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getString(2));
        }
        return map;
    }

    public int countByMovieAndType(Connection con, int movieId, String type) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM MovieMedia WHERE movie_id = ? AND media_type = ?")) {
            ps.setInt(1, movieId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int insert(Connection con, MovieMedia m) throws SQLException {
        String sql = "INSERT INTO MovieMedia (movie_id, media_type, url, caption, is_primary) VALUES (?, ?, ?, ?, ?)";
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
                "UPDATE MovieMedia SET is_primary = 0 WHERE movie_id = ? AND media_type = ? AND is_primary = 1")) {
            ps.setInt(1, movieId);
            ps.setString(2, type);
            ps.executeUpdate();
        }
    }

    public void setPrimary(Connection con, int mediaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE MovieMedia SET is_primary = 1 WHERE media_id = ?")) {
            ps.setInt(1, mediaId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int mediaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM MovieMedia WHERE media_id = ?")) {
            ps.setInt(1, mediaId);
            ps.executeUpdate();
        }
    }

    private List<MovieMedia> mapList(PreparedStatement ps) throws SQLException {
        List<MovieMedia> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MovieMedia m = new MovieMedia();
                m.setMediaId(rs.getInt("media_id"));
                m.setMovieId(rs.getInt("movie_id"));
                m.setMediaType(rs.getString("media_type"));
                m.setUrl(rs.getString("url"));
                m.setCaption(rs.getString("caption"));
                m.setPrimary(rs.getBoolean("is_primary"));
                m.setCreatedAt(JdbcUtil.getDateTime(rs, "created_at"));
                list.add(m);
            }
        }
        return list;
    }
}
