package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bảng MovieGenres. */
public class MovieGenreDAO {

    public Set<Integer> findGenreIdsByMovie(Connection con, int movieId) throws SQLException {
        Set<Integer> ids = new LinkedHashSet<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT genre_id FROM MovieGenres WHERE movie_id = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /** movie_id -> danh sách tên thể loại, cho mọi phim. */
    public Map<Integer, List<String>> findGenreNamesPerMovie(Connection con) throws SQLException {
        String sql = "SELECT mg.movie_id, g.name FROM MovieGenres mg "
                + "JOIN Genres g ON g.genre_id = mg.genre_id ORDER BY g.name";
        Map<Integer, List<String>> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getInt("movie_id"), k -> new ArrayList<>()).add(rs.getString("name"));
            }
        }
        return map;
    }

    /** genre_id -> số phim thuộc thể loại. */
    public Map<Integer, Integer> countMoviesPerGenre(Connection con) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT genre_id, COUNT(*) FROM MovieGenres GROUP BY genre_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
        }
        return map;
    }

    public int countByGenre(Connection con, int genreId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM MovieGenres WHERE genre_id = ?")) {
            ps.setInt(1, genreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countByMovie(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM MovieGenres WHERE movie_id = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Các phim mà thể loại này là thể loại duy nhất (gỡ đi thì phim không còn thể loại nào). */
    public List<Integer> findMoviesWithOnlyGenre(Connection con, int genreId) throws SQLException {
        String sql = "SELECT mg.movie_id FROM MovieGenres mg WHERE mg.genre_id = ? AND NOT EXISTS "
                + "(SELECT 1 FROM MovieGenres o WHERE o.movie_id = mg.movie_id AND o.genre_id <> ?)";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, genreId);
            ps.setInt(2, genreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /** Gỡ thể loại khỏi mọi phim. Trả về số phim bị gỡ. */
    public int deleteByGenre(Connection con, int genreId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM MovieGenres WHERE genre_id = ?")) {
            ps.setInt(1, genreId);
            return ps.executeUpdate();
        }
    }

    /** Thay toàn bộ thể loại của phim. Gọi trong transaction. */
    public void replaceForMovie(Connection con, int movieId, Collection<Integer> genreIds) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM MovieGenres WHERE movie_id = ?")) {
            del.setInt(1, movieId);
            del.executeUpdate();
        }
        try (PreparedStatement ins = con.prepareStatement(
                "INSERT INTO MovieGenres (movie_id, genre_id) VALUES (?, ?)")) {
            for (Integer genreId : genreIds) {
                ins.setInt(1, movieId);
                ins.setInt(2, genreId);
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }
}
