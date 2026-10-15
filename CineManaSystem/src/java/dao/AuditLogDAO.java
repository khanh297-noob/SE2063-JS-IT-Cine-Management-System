package dao;

import model.AuditLog;
import util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public List<AuditLog> getAllLogs() {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT a.LogID, a.UserID, u.FullName, a.Action, a.Timestamp, a.IPAddress "
                + "FROM AuditLog a "
                + "JOIN [User] u ON a.UserID = u.UserID "
                + "ORDER BY a.Timestamp DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AuditLog log = new AuditLog(
                        rs.getInt("LogID"),
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Action"),
                        rs.getTimestamp("Timestamp"),
                        rs.getString("IPAddress")
                );
                list.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}