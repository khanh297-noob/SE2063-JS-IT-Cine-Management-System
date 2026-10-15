package dto;

import java.io.Serializable;

/**
 * Thông tin đăng ký đang chờ xác thực OTP. Được giữ trong session;
 * tài khoản chỉ được tạo trong bảng [User] (trạng thái Active) SAU KHI nhập đúng OTP.
 */
public class PendingRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fullName;
    private final String email;
    private final String phone;
    private final String passwordHash;

    public PendingRegistration(String fullName, String email, String phone, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
}
