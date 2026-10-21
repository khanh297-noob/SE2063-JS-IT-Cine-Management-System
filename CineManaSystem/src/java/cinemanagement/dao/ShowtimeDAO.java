package cinemanagement.dao;

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

/**
 * Bảng Showtime - CHỈ ĐỌC.
 * Bảng thuộc module Showtime; ở đây chỉ đếm suất chiếu để chặn các thao tác
 * làm hỏng lịch chiếu (gỡ phòng, gỡ chi nhánh, ngưng chiếu phim...).
 * "Sắp tới" = ShowDate từ hôm nay trở đi và chưa bị huỷ (Status <> 'Cancelled').
 * countByMovie / countByRoom đếm cả suất đã huỷ vì khoá ngoại vẫn chặn xoá phim/phòng còn suất chiếu.
 */
public class ShowtimeDAO {

    private static final String UPCOMING = " AND s.ShowDate >= CAST(GETDATE() AS DATE) AND s.Status <> 'Cancelled'";

    public int countByMovie(Connection con, int movieId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.MovieID = ?", movieId);
    }

    public int countUpcomingByMovie(Connection con, int movieId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.MovieID = ?" + UPCOMING, movieId);
    }

    public int countByRoom(Connection con, int roomId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.RoomID = ?", roomId);
    }

    public int countUpcomingByRoom(Connection con, int roomId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.RoomID = ?" + UPCOMING, roomId);
    }

    public int countUpcomingByBranch(Connection con, int branchId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE r.BranchID = ?" + UPCOMING, branchId);
    }

    /** BranchID -> số suất chiếu sắp tới của phim. */
    public Map<Integer, Integer> countUpcomingByMovieGroupedByBranch(Connection con, int movieId) throws SQLException {
        String sql = "SELECT r.BranchID, COUNT(*) FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE s.MovieID = ?" + UPCOMING + " GROUP BY r.BranchID";
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
            }
        }
        return map;
    }

    /**
     * Suất chiếu sắp tới của phim tại chi nhánh. Nếu có from/to thì chỉ đếm
     * những suất nằm NGOÀI khoảng [from, to].
     */
    public int countUpcomingAtBranch(Connection con, int movieId, int branchId,
                                     LocalDate from, LocalDate to) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE s.MovieID = ? AND r.BranchID = ?" + UPCOMING);
        List<Object> params = new ArrayList<>(List.of(movieId, branchId));
        if (from != null && to != null) {
            sql.append(" AND (s.ShowDate < ? OR s.ShowDate > ?)");
            params.add(Date.valueOf(from));
            params.add(Date.valueOf(to));
        } else if (from != null) {
            sql.append(" AND s.ShowDate < ?");
            params.add(Date.valueOf(from));
        } else if (to != null) {
            sql.append(" AND s.ShowDate > ?");
            params.add(Date.valueOf(to));
        }
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int count(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
