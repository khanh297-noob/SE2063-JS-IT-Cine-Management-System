package cinemanagement.dao.movie;

import cinemanagement.model.movie.Genre;
import cinemanagement.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Bảng Genre. */
public class GenreDAO {

    public List<Genre> findAll(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT GenreID, name, description FROM Genre ORDER BY name")) {
            return mapList(ps);
        }
    }

    public Genre findById(Connection con, int genreId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT GenreID, name, description FROM Genre WHERE GenreID = ?")) {
            ps.setInt(1, genreId);
            List<Genre> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public int insert(Connection con, Genre g) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Genre (name, description) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getName());
            JdbcUtil.setString(ps, 2, g.getDescription());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    public void update(Connection con, Genre g) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Genre SET name = ?, description = ? WHERE GenreID = ?")) {
            ps.setString(1, g.getName());
            JdbcUtil.setString(ps, 2, g.getDescription());
            ps.setInt(3, g.getGenreId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int genreId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Genre WHERE GenreID = ?")) {
            ps.setInt(1, genreId);
            ps.executeUpdate();
        }
    }

    private List<Genre> mapList(PreparedStatement ps) throws SQLException {
        List<Genre> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Genre g = new Genre();
                g.setGenreId(rs.getInt("GenreID"));
                g.setName(rs.getString("name"));
                g.setDescription(rs.getString("description"));
                list.add(g);
            }
        }
        return list;
    }
}
