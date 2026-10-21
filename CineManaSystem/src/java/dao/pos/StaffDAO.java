package dao.pos;

import model.User;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn nhân viên cho phần POS / giờ làm.
 * Đăng nhập, đăng ký... dùng dao.UserDao của nhóm.
 */
public class StaffDAO extends DBContext {

    // Lấy user theo email (kèm PasswordHash để kiểm tra mật khẩu quản lý khi hoàn tiền)
    public User getByEmail(String email) {
        String sql = "SELECT UserID, FullName, Email, Phone, PasswordHash, Role, Status FROM [User] "
                + "WHERE LOWER(Email) = LOWER(?)";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = mapUser(rs);
                    u.setPasswordHash(rs.getString("PasswordHash"));
                    return u;
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getByEmail: " + e.getMessage());
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
