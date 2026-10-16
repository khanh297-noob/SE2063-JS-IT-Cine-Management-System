package dao;

import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.User;

public class UserDAO extends DBContext {

    // Đăng nhập: đúng email + mật khẩu thì trả về User, sai thì trả về null
    public User login(String email, String password) {
        String sql = "SELECT UserID, FullName, Email, Phone, Role, Status FROM [User] "
                + "WHERE Email = ? AND PasswordHash = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi login: " + e.getMessage());
        }
        return null;
    }

    // Danh sách nhân viên quầy + soát vé (để lọc giờ làm)
    public List<User> getStaffList() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT UserID, FullName, Email, Phone, Role, Status FROM [User] "
                + "WHERE Role IN ('Ticket Cashier', 'Ticket Checker') ORDER BY FullName";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getStaffList: " + e.getMessage());
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("UserID"));
        u.setFullName(rs.getString("FullName"));
        u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone"));
        u.setRole(rs.getString("Role"));
        u.setStatus(rs.getString("Status"));
        return u;
    }
}
