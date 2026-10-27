package dao.movie;

import model.movie.MovieBranch;
import util.JdbcUtil;

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

/** Bảng MovieBranch. */
public class MovieBranchDAO {

    public List<MovieBranch> findByMovie(Connection con, int movieId) throws SQLException {
        String sql = "SELECT MovieID, BranchID, AvailableFrom, AvailableTo, AssignedAt "
                + "FROM MovieBranch WHERE MovieID = ?";
        List<MovieBranch> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovieBranch mb = new MovieBranch();
                    mb.setMovieId(rs.getInt("MovieID"));
                    mb.setBranchId(rs.getInt("BranchID"));
                    mb.setAvailableFrom(JdbcUtil.getDate(rs, "AvailableFrom"));
                    mb.setAvailableTo(JdbcUtil.getDate(rs, "AvailableTo"));
                    mb.setAssignedAt(JdbcUtil.getDateTime(rs, "AssignedAt"));
                    list.add(mb);
                }
            }
        }
        return list;
    }

    /** MovieID -> số chi nhánh đã gán. */
    public Map<Integer, Integer> countBranchesPerMovie(Connection con) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT MovieID, COUNT(*) FROM MovieBranch GROUP BY MovieID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
        }
        return map;
    }

    public int countByMovie(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM MovieBranch WHERE MovieID = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Số chi nhánh thực sự chiếu được phim vào ngày date: chi nhánh đang hoạt động và date nằm trong
     * khung AvailableFrom..AvailableTo (để trống = không giới hạn). Dùng cho BR-MS-01.
     */
    public int countScreeningByMovie(Connection con, int movieId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM MovieBranch mb JOIN Branch b ON b.BranchID = mb.BranchID "
                + "WHERE mb.MovieID = ? AND b.Status = 'Active' "
                + "AND (mb.AvailableFrom IS NULL OR mb.AvailableFrom <= ?) "
                + "AND (mb.AvailableTo IS NULL OR mb.AvailableTo >= ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setDate(2, Date.valueOf(date));
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void insert(Connection con, MovieBranch mb) throws SQLException {
        String sql = "INSERT INTO MovieBranch (MovieID, BranchID, AvailableFrom, AvailableTo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mb.getMovieId());
            ps.setInt(2, mb.getBranchId());
            JdbcUtil.setDate(ps, 3, mb.getAvailableFrom());
            JdbcUtil.setDate(ps, 4, mb.getAvailableTo());
            ps.executeUpdate();
        }
    }

    public void updateWindow(Connection con, MovieBranch mb) throws SQLException {
        String sql = "UPDATE MovieBranch SET AvailableFrom = ?, AvailableTo = ?, AssignedAt = SYSDATETIME() "
                + "WHERE MovieID = ? AND BranchID = ?";
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
                "DELETE FROM MovieBranch WHERE MovieID = ? AND BranchID = ?")) {
            ps.setInt(1, movieId);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    /** Cho module Showtime: phim có được chiếu tại chi nhánh vào ngày đó không. */
    public boolean isAvailable(Connection con, int movieId, int branchId, LocalDate date) throws SQLException {
        String sql = "SELECT 1 FROM MovieBranch WHERE MovieID = ? AND BranchID = ? "
                + "AND (AvailableFrom IS NULL OR AvailableFrom <= ?) "
                + "AND (AvailableTo IS NULL OR AvailableTo >= ?)";
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
