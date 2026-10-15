package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import model.User;

public class UserDao extends DBContext {

    public User findByEmail(String email) {
        if (email == null) return null;
        try {
            String sql = "SELECT * FROM [User] WHERE LOWER(Email) = LOWER(?)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, email.trim());

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return mapRowUser(rs);
            }
        } catch (Exception e) {
            System.err.println("UserDao.findByEmail error: " + e.getMessage());
        }
        return null;
    }

    public User findByPhone(String phone) {
        if (phone == null) return null;
        try {
            String sql = "SELECT * FROM [User] WHERE Phone = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, phone.trim());

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return mapRowUser(rs);
            }
        } catch (Exception e) {
            System.err.println("UserDao.findByPhone error: " + e.getMessage());
        }
        return null;
    }

    public User findById(int userId) {
        try {
            String sql = "SELECT * FROM [User] WHERE UserID = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, userId);

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return mapRowUser(rs);
            }
        } catch (Exception e) {
            System.err.println("UserDao.findById error: " + e.getMessage());
        }
        return null;
    }

    public boolean insertUser(User user) {
        try {
            String sql = "INSERT INTO [User] (FullName, Email, Phone, PasswordHash, [Role], [Status]) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, user.getFullName());
            st.setString(2, user.getEmail().trim().toLowerCase());
            st.setString(3, user.getPhone().trim());
            st.setString(4, user.getPasswordHash());
            st.setString(5, user.getRole() != null ? user.getRole() : "Customer");
            st.setString(6, user.getStatus() != null ? user.getStatus() : "Active");

            int rows = st.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.err.println("UserDao.insertUser error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatusByEmail(String email, String status) {
        try {
            String sql = "UPDATE [User] SET [Status] = ? WHERE LOWER(Email) = LOWER(?)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, status);
            st.setString(2, email.trim());

            return st.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("UserDao.updateStatusByEmail error: " + e.getMessage());
            return false;
        }
    }

    /** Đóng kết nối khi DAO được tạo cho một lần dùng (ví dụ trong filter). */
    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {
        }
    }

    public boolean updateProfile(int userId, String fullName, String phone) {
        try {
            String sql = "UPDATE [User] SET FullName = ?, Phone = ? WHERE UserID = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, fullName);
            st.setString(2, phone);
            st.setInt(3, userId);

            return st.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("UserDao.updateProfile error: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(int userId, String newPasswordHash) {
        try {
            String sql = "UPDATE [User] SET PasswordHash = ? WHERE UserID = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, newPasswordHash);
            st.setInt(2, userId);

            return st.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("UserDao.changePassword error: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // OTP & EMAIL VERIFICATION
    // ==========================================

    public boolean saveOtp(String email, String otpCode, int expiryMinutes) {
        try {
            // Vô hiệu hóa các mã OTP cũ chưa dùng
            String disableOldSql = "UPDATE EmailVerification SET IsUsed = 1 WHERE LOWER(Email) = LOWER(?) AND IsUsed = 0";
            PreparedStatement stOld = connection.prepareStatement(disableOldSql);
            stOld.setString(1, email.trim());
            stOld.executeUpdate();

            // Thêm mã OTP mới
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, expiryMinutes);
            Timestamp expiryTime = new Timestamp(cal.getTimeInMillis());

            String sql = "INSERT INTO EmailVerification (Email, OtpCode, ExpiryTime, IsUsed) VALUES (?, ?, ?, 0)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, email.trim().toLowerCase());
            st.setString(2, otpCode.trim());
            st.setTimestamp(3, expiryTime);

            return st.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("UserDao.saveOtp error: " + e.getMessage());
            return false;
        }
    }

    /** Kiểm tra OTP còn hạn, chưa dùng; nếu đúng thì đánh dấu đã dùng. Không thay đổi bảng [User]. */
    public boolean verifyOtp(String email, String otpCode) {
        try {
            String sql = "SELECT VerificationID FROM EmailVerification "
                       + "WHERE LOWER(Email) = LOWER(?) AND OtpCode = ? AND IsUsed = 0 AND ExpiryTime >= GETDATE()";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, email.trim());
            st.setString(2, otpCode.trim());

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("VerificationID");
                String updateSql = "UPDATE EmailVerification SET IsUsed = 1 WHERE VerificationID = ?";
                PreparedStatement updateSt = connection.prepareStatement(updateSql);
                updateSt.setInt(1, id);
                return updateSt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("UserDao.verifyOtp error: " + e.getMessage());
        }
        return false;
    }

    private User mapRowUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserID(rs.getInt("UserID"));
        u.setFullName(rs.getString("FullName"));
        u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setRole(rs.getString("Role"));
        u.setStatus(rs.getString("Status"));
        return u;
    }
}
