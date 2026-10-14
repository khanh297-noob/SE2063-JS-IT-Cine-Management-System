package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cinemanagement.util.DBContext;

/**
 * Giu ghe tam thoi. Moi so sanh thoi gian dung GETDATE() cua SQL Server
 * (tranh lech dong ho giua JVM va DB). Ghe het han coi nhu tu dong nha ra.
 */
public class SeatHoldDAO {

    public int deleteExpired() throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM SeatHold WHERE ExpiresAt <= GETDATE()")) {
            return ps.executeUpdate();
        }
    }

    public void deleteExpiredForShowtime(Connection c, int showtimeId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM SeatHold WHERE ShowtimeID = ? AND ExpiresAt <= GETDATE()")) {
            ps.setInt(1, showtimeId);
            ps.executeUpdate();
        }
    }

    public void deleteByUserShowtime(Connection c, int userId, int showtimeId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM SeatHold WHERE UserID = ? AND ShowtimeID = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, showtimeId);
            ps.executeUpdate();
        }
    }

    public void deleteByUserShowtime(int userId, int showtimeId) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            deleteByUserShowtime(c, userId, showtimeId);
        }
    }

    public void deleteByUser(int userId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM SeatHold WHERE UserID = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void deleteByShowtime(Connection c, int showtimeId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM SeatHold WHERE ShowtimeID = ?")) {
            ps.setInt(1, showtimeId);
            ps.executeUpdate();
        }
    }

    /** Nem SQLException 2627 neu ghe da bi nguoi khac giu (UQ_SeatHold_Seat). */
    public void insert(Connection c, int showtimeId, int seatId, int userId, int minutes) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO SeatHold (ShowtimeID, SeatID, UserID, ExpiresAt) "
                + "VALUES (?, ?, ?, DATEADD(MINUTE, ?, GETDATE()))")) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, seatId);
            ps.setInt(3, userId);
            ps.setInt(4, minutes);
            ps.executeUpdate();
        }
    }

    /** seatId -> userId cua cac ghe dang duoc giu (con han). */
    public Map<Integer, Integer> findActiveByShowtime(int showtimeId) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT SeatID, UserID FROM SeatHold WHERE ShowtimeID = ? AND ExpiresAt > GETDATE()")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt(1), rs.getInt(2));
                }
            }
        }
        return map;
    }

    public List<Integer> activeSeatsOfUser(int userId, int showtimeId) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            return activeSeatsOfUser(c, userId, showtimeId, false);
        }
    }

    /** lock = true: khoa cac dong hold (dung khi xac nhan thanh toan). */
    public List<Integer> activeSeatsOfUser(Connection c, int userId, int showtimeId, boolean lock) throws SQLException {
        String sql = "SELECT SeatID FROM SeatHold" + (lock ? " WITH (UPDLOCK, HOLDLOCK)" : "")
                + " WHERE UserID = ? AND ShowtimeID = ? AND ExpiresAt > GETDATE()";
        List<Integer> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt(1));
                }
            }
        }
        return list;
    }

    /** So giay con lai cua lan giu ghe; -1 neu khong con ghe nao duoc giu. */
    public int remainingSeconds(int userId, int showtimeId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT MIN(DATEDIFF(SECOND, GETDATE(), ExpiresAt)) "
                     + "FROM SeatHold WHERE UserID = ? AND ShowtimeID = ? AND ExpiresAt > GETDATE()")) {
            ps.setInt(1, userId);
            ps.setInt(2, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt(1);
                    return rs.wasNull() ? -1 : Math.max(v, 0);
                }
                return -1;
            }
        }
    }
}
