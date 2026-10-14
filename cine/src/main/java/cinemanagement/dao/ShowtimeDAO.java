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
 * Bảng Showtimes - CHỈ ĐỌC.
 * Bảng thuộc module Showtime; ở đây chỉ đếm suất chiếu để chặn các thao tác
 * làm hỏng lịch chiếu (gỡ phòng, gỡ chi nhánh, ngưng chiếu phim...).
 * "Sắp tới" = show_date từ hôm nay trở đi.
 */
public class ShowtimeDAO {

    private static final String UPCOMING = " AND s.show_date >= CAST(GETDATE() AS DATE)";

    public int countByMovie(Connection con, int movieId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtimes s WHERE s.movie_id = ?", movieId);
    }

    public int countUpcomingByMovie(Connection con, int movieId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtimes s WHERE s.movie_id = ?" + UPCOMING, movieId);
    }

    public int countByRoom(Connection con, int roomId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtimes s WHERE s.room_id = ?", roomId);
    }

    public int countUpcomingByRoom(Connection con, int roomId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtimes s WHERE s.room_id = ?" + UPCOMING, roomId);
    }

    public int countUpcomingByBranch(Connection con, int branchId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtimes s JOIN Rooms r ON r.room_id = s.room_id "
                + "WHERE r.branch_id = ?" + UPCOMING, branchId);
    }

    /** branch_id -> số suất chiếu sắp tới của phim. */
    public Map<Integer, Integer> countUpcomingByMovieGroupedByBranch(Connection con, int movieId) throws SQLException {
        String sql = "SELECT r.branch_id, COUNT(*) FROM Showtimes s JOIN Rooms r ON r.room_id = s.room_id "
                + "WHERE s.movie_id = ?" + UPCOMING + " GROUP BY r.branch_id";
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
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Showtimes s JOIN Rooms r ON r.room_id = s.room_id "
                + "WHERE s.movie_id = ? AND r.branch_id = ?" + UPCOMING);
        List<Object> params = new ArrayList<>(List.of(movieId, branchId));
        if (from != null && to != null) {
            sql.append(" AND (s.show_date < ? OR s.show_date > ?)");
            params.add(Date.valueOf(from));
            params.add(Date.valueOf(to));
        } else if (from != null) {
            sql.append(" AND s.show_date < ?");
            params.add(Date.valueOf(from));
        } else if (to != null) {
            sql.append(" AND s.show_date > ?");
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
