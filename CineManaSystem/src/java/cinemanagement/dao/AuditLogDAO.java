package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cinemanagement.util.DBContext;

public class AuditLogDAO {
    private static final Logger LOG = Logger.getLogger(AuditLogDAO.class.getName());

    public void log(int userId, String action, String ip) {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO AuditLog (UserID, Action, IPAddress) VALUES (?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, action.length() > 500 ? action.substring(0, 500) : action);
            ps.setString(3, ip == null ? "unknown" : ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Khong ghi duoc AuditLog", e);
        }
    }
}
