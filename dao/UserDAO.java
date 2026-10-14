package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cinemanagement.model.User;
import cinemanagement.util.DBContext;

public class UserDAO {

    private static final String BASE = "SELECT u.*, b.Name AS BranchName FROM [User] u "
            + "LEFT JOIN Branch b ON b.BranchID = u.BranchID ";

    public User findById(int id) throws SQLException {
        return findOne(BASE + "WHERE u.UserID = ?", id);
    }

    public User findByEmail(String email) throws SQLException {
        return findOne(BASE + "WHERE u.Email = ?", email);
    }

    /** POS: tim khach hang theo SDT hoac email. */
    public User findCustomerByPhoneOrEmail(String key) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT TOP 1 u.*, NULL AS BranchName FROM [User] u WHERE u.Role = 'Customer' AND (u.Phone = ? OR u.Email = ?) "
                     + "ORDER BY u.UserID")) {
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void updateProfile(int userId, String fullName, String phone) throws SQLException {
        update("UPDATE [User] SET FullName = ?, Phone = ? WHERE UserID = ?", fullName, phone, userId);
    }

    public void updatePassword(int userId, String hash) throws SQLException {
        update("UPDATE [User] SET PasswordHash = ? WHERE UserID = ?", hash, userId);
    }

    public void updateStatus(int userId, String status) throws SQLException {
        update("UPDATE [User] SET Status = ? WHERE UserID = ?", status, userId);
    }

    private void update(String sql, Object... params) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    private User findOne(String sql, Object param) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("UserID"));
        u.setFullName(rs.getString("FullName"));
        u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setRole(rs.getString("Role"));
        u.setStatus(rs.getString("Status"));
        int branch = rs.getInt("BranchID");
        u.setBranchId(rs.wasNull() ? null : branch);
        u.setBranchName(rs.getString("BranchName"));
        return u;
    }
}
