package dao;

import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.AuditLog;

/**
 * Bảng AuditLog: nhật ký thao tác. Phần POS còn dùng bảng này để lưu
 * ca làm việc (SHIFT_START / SHIFT_END) và hoàn tiền (COUNTER_REFUND) vì DB chưa có bảng riêng.
 */
public class AuditLogDAO extends DBContext {

    private static final String SELECT_LOG
            = "SELECT a.LogID, a.UserID, u.FullName, u.Role, a.Action, a.Timestamp "
            + "FROM AuditLog a JOIN [User] u ON u.UserID = a.UserID ";

    public void insert(int userId, String action, String ip) {
        String sql = "INSERT INTO AuditLog (UserID, Action, IPAddress, Timestamp) VALUES (?, ?, ?, GETDATE())";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action.length() > 255 ? action.substring(0, 255) : action);
            ps.setString(3, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Lỗi AuditLogDAO.insert: " + e.getMessage());
        }
    }

    /**
     * Lấy các log có Action bắt đầu bằng typePrefix (vd "SHIFT_" hoặc "COUNTER_REFUND")
     * trong khoảng [from, to). userId = 0 nghĩa là lấy của tất cả nhân viên.
     */
    public List<AuditLog> getLogs(int userId, String typePrefix, Timestamp from, Timestamp to) {
        List<AuditLog> list = new ArrayList<>();
        String sql = SELECT_LOG + "WHERE a.Action LIKE ? AND a.Timestamp >= ? AND a.Timestamp < ? "
                + (userId > 0 ? "AND a.UserID = ? " : "")
                + "ORDER BY a.Timestamp, a.LogID";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, typePrefix + "%");
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, to);
            if (userId > 0) {
                ps.setInt(4, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getLogs: " + e.getMessage());
        }
        return list;
    }

    // Log SHIFT_START / SHIFT_END gần nhất của nhân viên
    public AuditLog getLastShiftLog(int userId) {
        String sql = "SELECT TOP 1 a.LogID, a.UserID, u.FullName, u.Role, a.Action, a.Timestamp "
                + "FROM AuditLog a JOIN [User] u ON u.UserID = a.UserID "
                + "WHERE a.UserID = ? AND a.Action LIKE 'SHIFT_%' ORDER BY a.Timestamp DESC, a.LogID DESC";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLog(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getLastShiftLog: " + e.getMessage());
        }
        return null;
    }

    private AuditLog mapLog(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setLogId(rs.getInt("LogID"));
        a.setUserId(rs.getInt("UserID"));
        a.setFullName(rs.getString("FullName"));
        a.setRole(rs.getString("Role"));
        a.setAction(rs.getString("Action"));
        a.setTime(rs.getTimestamp("Timestamp"));
        return a;
    }
}
