package dao.admin;

import dao.DBContext;
import model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public List<AuditLog> getAllLogs() throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT a.LogID, a.UserID, u.FullName, a.Action, a.Timestamp, a.IPAddress "
                + "FROM AuditLog a "
                + "LEFT JOIN [User] u ON a.UserID = u.UserID "
                + "ORDER BY a.Timestamp DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AuditLog log = new AuditLog(
                        rs.getInt("LogID"),
                        (Integer) rs.getObject("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Action"),
                        rs.getTimestamp("Timestamp"),
                        rs.getString("IPAddress")
                );
                list.add(log);
            }
        } catch (SQLException e) {
            throw e;
        }
        return list;
    }
    public void insert(Connection conn,int actorId,String action,String ip) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO dbo.AuditLog(UserID,Action,IPAddress) VALUES(?,?,?)")) {
            ps.setInt(1,actorId); ps.setNString(2,action); ps.setString(3,ip);
            ps.executeUpdate();
        }
    }
}