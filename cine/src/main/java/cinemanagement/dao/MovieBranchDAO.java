package cinemanagement.dao;

import cinemanagement.model.MovieBranch;
import cinemanagement.util.JdbcUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bảng MovieBranches. */
public class MovieBranchDAO {

    public List<MovieBranch> findByMovie(Connection con, int movieId) throws SQLException {
        String sql = "SELECT movie_id, branch_id, available_from, available_to, assigned_at "
                + "FROM MovieBranches WHERE movie_id = ?";
        List<MovieBranch> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovieBranch mb = new MovieBranch();
                    mb.setMovieId(rs.getInt("movie_id"));
                    mb.setBranchId(rs.getInt("branch_id"));
                    mb.setAvailableFrom(JdbcUtil.getDate(rs, "available_from"));
                    mb.setAvailableTo(JdbcUtil.getDate(rs, "available_to"));
                    mb.setAssignedAt(JdbcUtil.getDateTime(rs, "assigned_at"));
                    list.add(mb);
                }
            }
        }
        return list;
    }

    /** movie_id -> số chi nhánh đã gán. */
    public Map<Integer, Integer> countBranchesPerMovie(Connection con) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT movie_id, COUNT(*) FROM MovieBranches GROUP BY movie_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
        }
        return map;
    }

    public int countByMovie(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM MovieBranches WHERE movie_id = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void insert(Connection con, MovieBranch mb) throws SQLException {
        String sql = "INSERT INTO MovieBranches (movie_id, branch_id, available_from, available_to) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mb.getMovieId());
            ps.setInt(2, mb.getBranchId());
            JdbcUtil.setDate(ps, 3, mb.getAvailableFrom());
            JdbcUtil.setDate(ps, 4, mb.getAvailableTo());
            ps.executeUpdate();
        }
    }

    public void updateWindow(Connection con, MovieBranch mb) throws SQLException {
        String sql = "UPDATE MovieBranches SET available_from = ?, available_to = ?, assigned_at = SYSDATETIME() "
                + "WHERE movie_id = ? AND branch_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            JdbcUtil.setDate(ps, 1, mb.getAvailableFrom());
            JdbcUtil.setDate(ps, 2, mb.getAvailableTo());
            ps.setInt(3, mb.getMovieId());
            ps.setInt(4, mb.getBranchId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int movieId, int branchId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM MovieBranches WHERE movie_id = ? AND branch_id = ?")) {
            ps.setInt(1, movieId);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    /** Cho module Showtime: phim có được chiếu tại chi nhánh vào ngày đó không. */
    public boolean isAvailable(Connection con, int movieId, int branchId, LocalDate date) throws SQLException {
        String sql = "SELECT 1 FROM MovieBranches WHERE movie_id = ? AND branch_id = ? "
                + "AND (available_from IS NULL OR available_from <= ?) "
                + "AND (available_to IS NULL OR available_to >= ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, branchId);
            ps.setDate(3, Date.valueOf(date));
            ps.setDate(4, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
