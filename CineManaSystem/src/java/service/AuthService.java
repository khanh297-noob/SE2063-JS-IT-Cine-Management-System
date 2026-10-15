package service;

import dao.UserDao;
import model.User;
import util.OtpGuard;
import util.OtpUtil;
import util.PasswordUtil;
import java.io.Serializable;

public class AuthService {

    private final UserDao userDao = new UserDao();
    private final EmailService emailService = new EmailService();
    private static final int OTP_EXPIRY_MINUTES = 15;

    public static class AuthResult {

        public final boolean success;
        public final String message;
        public final User user;
        public final String otpCode;
        /** Có giá trị khi bước đăng ký thành công và đang chờ xác thực OTP. */
        public PendingRegistration pending;

        public AuthResult(boolean success, String message, User user) {
            this(success, message, user, null);
        }

        public AuthResult(boolean success, String message, User user, String otpCode) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.otpCode = otpCode;
        }
    }

    public AuthResult login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return new AuthResult(false, "Vui lòng nhập Email!", null);
        }
        if (password == null || password.trim().isEmpty()) {
            return new AuthResult(false, "Mật khẩu không được để trống!", null);
        }

        User user = userDao.findByEmail(email.trim());
        if (user == null) {
            return new AuthResult(false, "Tài khoản chưa được đăng ký!", null);
        }
        // Kiểm tra mật khẩu trước để không lộ trạng thái tài khoản cho người không biết mật khẩu
        if (!PasswordUtil.verify(password.trim(), user.getPasswordHash())) {
            return new AuthResult(false, "Mật khẩu không đúng. Vui lòng thử lại!", null);
        }
        String status = user.getStatus();
        // Chỉ tài khoản Active mới được đăng nhập (Deactivated, Locked và mọi trạng thái khác đều bị chặn)
        if (!"Active".equalsIgnoreCase(status)) {
            return new AuthResult(false, "Tài khoản của bạn đã bị khóa hoặc vô hiệu hóa. Vui lòng liên hệ hỗ trợ!", null);
        }
        return new AuthResult(true, "Đăng nhập thành công!", user);
    }

    /**
     * Bước 1 đăng ký: kiểm tra dữ liệu, gửi OTP đến email. CHƯA tạo tài khoản.
     * Khi thành công, result.pending chứa thông tin cần giữ trong session để xác thực OTP.
     */
    public AuthResult register(String fullName, String email, String phone, String password, String confirmPassword) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new AuthResult(false, "Họ và tên không được để trống!", null);
        }
        if (fullName.trim().length() > 150) {
            return new AuthResult(false, "Họ và tên quá dài (tối đa 150 ký tự)!", null);
        }
        if (email == null || email.trim().isEmpty()) {
            return new AuthResult(false, "Email không được để trống!", null);
        }
        if (!email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return new AuthResult(false, "Định dạng email không hợp lệ!", null);
        }
        if (email.trim().length() > 100) {
            return new AuthResult(false, "Email quá dài (tối đa 100 ký tự)!", null);
        }
        if (phone == null || phone.trim().isEmpty()) {
            return new AuthResult(false, "Số điện thoại không được để trống!", null);
        }
        if (!phone.trim().matches("^(0|\\+84)[0-9]{9}$")) {
            return new AuthResult(false, "Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng 0)!", null);
        }
        if (password == null || password.trim().isEmpty()) {
            return new AuthResult(false, "Mật khẩu không được để trống!", null);
        }
        if (password.length() < 6) {
            return new AuthResult(false, "Mật khẩu phải chứa ít nhất 6 ký tự!", null);
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            return new AuthResult(false, "Mật khẩu xác nhận không khớp!", null);
        }

        String emailNorm = email.trim().toLowerCase();

        if (userDao.findByEmail(emailNorm) != null) {
            return new AuthResult(false, "Email này đã được sử dụng. Vui lòng chọn email khác!", null);
        }
        if (userDao.findByPhone(phone.trim()) != null) {
            return new AuthResult(false, "Số điện thoại này đã được sử dụng. Vui lòng chọn số khác!", null);
        }

        long wait = OtpGuard.resendWaitSeconds(emailNorm);
        if (wait > 0) {
            return new AuthResult(false, "Vui lòng đợi " + wait + " giây trước khi yêu cầu mã OTP mới!", null);
        }

        PendingRegistration pending = new PendingRegistration(
                fullName.trim(), emailNorm, phone.trim(), PasswordUtil.hash(password));

        AuthResult sent = sendOtp(emailNorm, "Mã OTP đã được gửi đến email của bạn.");
        if (!sent.success) {
            return sent;
        }
        AuthResult result = new AuthResult(true, "Mã OTP đã được gửi đến email của bạn. Vui lòng nhập mã để hoàn tất đăng ký.", null);
        result.pending = pending;
        return result;
    }

    /** Tạo OTP mới, lưu DB và gửi email. */
    private AuthResult sendOtp(String email, String successMessage) {
        String otpCode = OtpUtil.generate6DigitOtp();

        if (!userDao.saveOtp(email, otpCode, OTP_EXPIRY_MINUTES)) {
            return new AuthResult(false, "Không thể lưu mã OTP. Vui lòng thử lại!", null);
        }
        if (!emailService.sendOtpEmail(email, otpCode)) {
            return new AuthResult(false, "Không thể gửi email OTP. Vui lòng kiểm tra địa chỉ email và thử lại!", null);
        }

        OtpGuard.markSent(email);
        OtpGuard.reset(email);
        return new AuthResult(true, successMessage, null);
    }

    /** Gửi lại OTP cho đăng ký đang chờ xác thực. */
    public AuthResult resendRegistrationOtp(PendingRegistration pending) {
        if (pending == null) {
            return new AuthResult(false, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại!", null);
        }
        String email = pending.getEmail();

        if (userDao.findByEmail(email) != null) {
            return new AuthResult(false, "Email này đã được đăng ký. Vui lòng đăng nhập!", null);
        }
        long wait = OtpGuard.resendWaitSeconds(email);
        if (wait > 0) {
            return new AuthResult(false, "Vui lòng đợi " + wait + " giây trước khi gửi lại mã OTP!", null);
        }
        return sendOtp(email, "Mã OTP mới đã được gửi đến email của bạn!");
    }

    /**
     * Bước 2 đăng ký: nhập đúng OTP thì mới tạo tài khoản (trạng thái Active).
     * Vì tài khoản chỉ được tạo ở bước này, OTP không thể dùng để kích hoạt lại
     * tài khoản đã bị Deactivated/Locked.
     */
    public AuthResult completeRegistration(PendingRegistration pending, String otpCode) {
        if (pending == null) {
            return new AuthResult(false, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại!", null);
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            return new AuthResult(false, "Vui lòng nhập mã xác thực OTP!", null);
        }
        String email = pending.getEmail();
        otpCode = otpCode.trim();

        if (!otpCode.matches("\\d{6}")) {
            return new AuthResult(false, "Mã OTP gồm đúng 6 chữ số!", null);
        }

        long lockSeconds = OtpGuard.lockRemainingSeconds(email);
        if (lockSeconds > 0) {
            return new AuthResult(false,
                    "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + ((lockSeconds + 59) / 60)
                    + " phút hoặc bấm gửi lại mã OTP mới!", null);
        }

        // Kiểm tra trùng trước khi tiêu hao OTP (có thể có người khác đã đăng ký email/số này trong lúc chờ)
        if (userDao.findByEmail(email) != null) {
            return new AuthResult(false, "Email này đã được đăng ký. Vui lòng đăng nhập!", null);
        }
        if (userDao.findByPhone(pending.getPhone()) != null) {
            return new AuthResult(false, "Số điện thoại này đã được sử dụng. Vui lòng đăng ký lại với số khác!", null);
        }

        if (!userDao.verifyOtp(email, otpCode)) {
            int left = OtpGuard.recordFailure(email);
            if (left > 0) {
                return new AuthResult(false,
                        "Mã OTP không chính xác hoặc đã hết hạn (" + OTP_EXPIRY_MINUTES + " phút)! Bạn còn "
                        + left + " lần thử.", null);
            }
            return new AuthResult(false,
                    "Bạn đã nhập sai quá " + OtpGuard.MAX_ATTEMPTS
                    + " lần. Vui lòng bấm gửi lại mã OTP mới!", null);
        }

        OtpGuard.reset(email);
        User newUser = new User(0, pending.getFullName(), email, pending.getPhone(),
                pending.getPasswordHash(), "Customer", "Active");
        if (!userDao.insertUser(newUser)) {
            return new AuthResult(false, "Không thể tạo tài khoản. Vui lòng đăng ký lại!", null);
        }
        return new AuthResult(true, "Xác thực thành công! Tài khoản của bạn đã được tạo.", userDao.findByEmail(email));
    }

    public AuthResult updateProfile(int userId, String fullName, String phone) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new AuthResult(false, "Họ và tên không được để trống!", null);
        }
        if (phone == null || phone.trim().isEmpty()) {
            return new AuthResult(false, "Số điện thoại không được để trống!", null);
        }
        if (!phone.trim().matches("^(0|\\+84)[0-9]{9}$")) {
            return new AuthResult(false, "Số điện thoại không hợp lệ!", null);
        }

        User existingPhone = userDao.findByPhone(phone.trim());
        if (existingPhone != null && existingPhone.getUserID() != userId) {
            return new AuthResult(false, "Số điện thoại này đã được tài khoản khác sử dụng!", null);
        }

        boolean ok = userDao.updateProfile(userId, fullName.trim(), phone.trim());
        if (ok) {
            User updated = userDao.findById(userId);
            return new AuthResult(true, "Cập nhật thông tin tài khoản thành công!", updated);
        }
        return new AuthResult(false, "Cập nhật thất bại. Vui lòng thử lại!", null);
    }

    public AuthResult changePassword(int userId, String oldPassword, String newPassword, String confirmPassword) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return new AuthResult(false, "Vui lòng nhập mật khẩu hiện tại!", null);
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return new AuthResult(false, "Vui lòng nhập mật khẩu mới!", null);
        }
        if (newPassword.length() < 6) {
            return new AuthResult(false, "Mật khẩu mới phải có ít nhất 6 ký tự!", null);
        }
        if (!newPassword.equals(confirmPassword)) {
            return new AuthResult(false, "Mật khẩu xác nhận không khớp!", null);
        }

        User user = userDao.findById(userId);
        if (user == null) {
            return new AuthResult(false, "Tài khoản không tồn tại!", null);
        }
        if (!PasswordUtil.verify(oldPassword, user.getPasswordHash())) {
            return new AuthResult(false, "Mật khẩu hiện tại không chính xác!", null);
        }

        String newHash = PasswordUtil.hash(newPassword);
        boolean ok = userDao.changePassword(userId, newHash);
        if (ok) {
            return new AuthResult(true, "Đổi mật khẩu thành công!", user);
        }
        return new AuthResult(false, "Đổi mật khẩu thất bại. Vui lòng thử lại!", null);
    }

    // ===== Nested data classes (previously package dto) =====

    /**
     * Thông tin đăng ký đang chờ xác thực OTP. Được giữ trong session;
     * tài khoản chỉ được tạo trong bảng [User] (trạng thái Active) SAU KHI nhập đúng OTP.
     */
    public static class PendingRegistration implements Serializable {

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
}
