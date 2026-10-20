package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import model.User;

/**
 * Truy vấn bảng [User] và bảng EmailVerification (mã OTP).
 */
public class UserDao extends DBContext {

    // ==================== BẢNG [User] ====================

    /** Tìm user theo email (không phân biệt hoa thường). Không thấy thì trả về null. */
    public User findByEmail(String email) {
        if (email == null) {
            return null;
        }
        String sql = "SELECT * FROM [User] WHERE LOWER(Email) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Tìm user theo số điện thoại. Không thấy thì trả về null. */
    public User findByPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String sql = "SELECT * FROM [User] WHERE Phone = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Tìm user theo mã. Không thấy thì trả về null. */
    public User findById(int userId) {
        String sql = "SELECT * FROM [User] WHERE UserID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Thêm user mới. Chưa có vai trò thì mặc định Customer, chưa có trạng thái thì mặc định Active. */
    public boolean insertUser(User user) {
        String sql = "INSERT INTO [User] (FullName, Email, Phone, PasswordHash, [Role], [Status]) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPhone().trim());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole() != null ? user.getRole() : "Customer");
            ps.setString(6, user.getStatus() != null ? user.getStatus() : "Active");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Cập nhật họ tên và số điện thoại. */
    public boolean updateProfile(int userId, String fullName, String phone) {
        String sql = "UPDATE [User] SET FullName = ?, Phone = ? WHERE UserID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Đổi mật khẩu (nhận vào mật khẩu đã mã hóa). */
    public boolean changePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE [User] SET PasswordHash = ? WHERE UserID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== BẢNG EmailVerification (OTP) ====================

    /** Lưu OTP mới. Các OTP cũ chưa dùng của email này bị vô hiệu hóa trước. */
    public boolean saveOtp(String email, String otpCode, int expiryMinutes) {
        // Bước 1: vô hiệu hóa các OTP cũ chưa dùng
        String disableOldSql = "UPDATE EmailVerification SET IsUsed = 1 "
                + "WHERE LOWER(Email) = LOWER(?) AND IsUsed = 0";
        try (PreparedStatement ps = connection.prepareStatement(disableOldSql)) {
            ps.setString(1, email.trim());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // Bước 2: tính thời điểm hết hạn
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, expiryMinutes);
        Timestamp expiryTime = new Timestamp(calendar.getTimeInMillis());

        // Bước 3: thêm OTP mới
        String insertSql = "INSERT INTO EmailVerification (Email, OtpCode, ExpiryTime, IsUsed) "
                + "VALUES (?, ?, ?, 0)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, otpCode.trim());
            ps.setTimestamp(3, expiryTime);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Kiểm tra OTP đúng, chưa dùng, chưa hết hạn. Nếu đúng thì đánh dấu đã dùng và trả về true. */
    public boolean verifyOtp(String email, String otpCode) {
        String selectSql = "SELECT VerificationID FROM EmailVerification "
                + "WHERE LOWER(Email) = LOWER(?) AND OtpCode = ? AND IsUsed = 0 AND ExpiryTime >= GETDATE()";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, email.trim());
            ps.setString(2, otpCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // OTP sai, đã dùng hoặc hết hạn
                }
                return markOtpUsed(rs.getInt("VerificationID"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Đánh dấu một OTP đã được sử dụng. */
    private boolean markOtpUsed(int verificationId) throws SQLException {
        String sql = "UPDATE EmailVerification SET IsUsed = 1 WHERE VerificationID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, verificationId);
            return ps.executeUpdate() > 0;
        }
    }

    // ==================== TIỆN ÍCH ====================

    /** Đóng kết nối (dùng khi DAO chỉ tạo ra cho một lần dùng, ví dụ trong filter). */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Chuyển một dòng kết quả SQL thành đối tượng User. */
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserID(rs.getInt("UserID"));
        user.setFullName(rs.getString("FullName"));
        user.setEmail(rs.getString("Email"));
        user.setPhone(rs.getString("Phone"));
        user.setPasswordHash(rs.getString("PasswordHash"));
        user.setRole(rs.getString("Role"));
        user.setStatus(rs.getString("Status"));
        int branchId = rs.getInt("BranchID");
        user.setBranchID(rs.wasNull() ? null : branchId);
        return user;
    }
}
