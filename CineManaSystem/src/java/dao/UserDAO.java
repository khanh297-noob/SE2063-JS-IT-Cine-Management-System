package dao;

import model.User;
import util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();

        String sql = "SELECT u.UserID, u.FullName, u.Email, u.Phone, u.PasswordHash, u.Role, u.Status, "
                + "u.BranchID, b.Name AS BranchName "
                + "FROM [User] u "
                + "LEFT JOIN Branch b ON u.BranchID = b.BranchID "
                + "ORDER BY u.UserID ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Integer branchId = (rs.getObject("BranchID") != null) ? rs.getInt("BranchID") : null;
                String branchName = rs.getString("BranchName");

                User u = new User(
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Email"),
                        rs.getString("Phone"),
                        rs.getString("PasswordHash"),
                        rs.getString("Role"),
                        rs.getString("Status"),
                        branchId,
                        branchName
                );
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateStatus(int userId, String newStatus) {
        String sql = "UPDATE [User] SET Status = ? WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateRole(int userId, String newRole) {
        String sql = "UPDATE [User] SET Role = ? WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}