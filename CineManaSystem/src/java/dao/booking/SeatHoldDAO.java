package dao.booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bảng SeatHold: ghế khách online đang giữ tạm (UNIQUE(ShowtimeID, SeatID) nên 1 ghế chỉ 1 người giữ được).
 * Mọi so sánh hết hạn dùng GETDATE() của SQL Server để không lệch đồng hồ giữa máy chạy web và DB.
 * POS (PosShowtimeDAO, BookingDAO.createCounterBooking) cũng đọc bảng này để không bán trùng ghế khách đang giữ.
 */
public class SeatHoldDAO {

    public int deleteExpired(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM SeatHold WHERE ExpiresAt <= GETDATE()")) {
            return ps.executeUpdate();
        }
    }

    public void deleteExpiredForShowtime(Connection con, int showtimeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM SeatHold WHERE ShowtimeID = ? AND ExpiresAt <= GETDATE()")) {
            ps.setInt(1, showtimeId);
            ps.executeUpdate();
        }
    }

    public void deleteByUserShowtime(Connection con, int userId, int showtimeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM SeatHold WHERE UserID = ? AND ShowtimeID = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, showtimeId);
            ps.executeUpdate();
        }
    }

    public void deleteByUser(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM SeatHold WHERE UserID = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void deleteByShowtime(Connection con, int showtimeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM SeatHold WHERE ShowtimeID = ?")) {
            ps.setInt(1, showtimeId);
            ps.executeUpdate();
        }
    }

    public void insert(Connection con, int showtimeId, int seatId, int userId, int minutes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO SeatHold (ShowtimeID, SeatID, UserID, ExpiresAt) "
                + "VALUES (?, ?, ?, DATEADD(MINUTE, ?, GETDATE()))")) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, seatId);
            ps.setInt(3, userId);
            ps.setInt(4, minutes);
            ps.executeUpdate();
        }
    }

    /** SeatID -> UserID của các ghế đang được giữ (chưa hết hạn) trong suất chiếu. */
    public Map<Integer, Integer> findActiveByShowtime(Connection con, int showtimeId) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT SeatID, UserID FROM SeatHold WHERE ShowtimeID = ? AND ExpiresAt > GETDATE()")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
            }
        }
        return map;
    }

    /** lock = true: khoá các dòng giữ ghế đến hết transaction (dùng khi tạo đơn từ ghế đang giữ). */
    public List<Integer> activeSeatsOfUser(Connection con, int userId, int showtimeId, boolean lock) throws SQLException {
        String sql = "SELECT SeatID FROM SeatHold" + (lock ? " WITH (UPDLOCK, HOLDLOCK)" : "")
                + " WHERE UserID = ? AND ShowtimeID = ? AND ExpiresAt > GETDATE()";
        List<Integer> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt(1));
            }
        }
        return list;
    }

    /** Số giây giữ ghế còn lại của khách trong suất chiếu, -1 nếu không còn giữ ghế nào. */
    public int remainingSeconds(Connection con, int userId, int showtimeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT MIN(DATEDIFF(SECOND, GETDATE(), ExpiresAt)) "
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
