package service;

import dao.UserDao;
import java.io.Serializable;
import model.User;
import thirdparty.EmailService;
import util.OtpGuard;
import util.OtpUtil;
import util.PasswordUtil;
import util.Validator;

/**
 * Xử lý nghiệp vụ tài khoản: đăng nhập, đăng ký (có OTP), quên mật khẩu,
 * cập nhật hồ sơ và đổi mật khẩu.
 *
 * Quy ước: dữ liệu sai thì ném IllegalArgumentException kèm thông báo tiếng Việt,
 * servlet bắt lỗi này và hiển thị thông báo cho người dùng.
 */
public class AuthService {

    // OTP có hiệu lực 15 phút
    private static final int OTP_EXPIRY_MINUTES = 15;

    private final UserDao userDao = new UserDao();
    private final EmailService emailService = new EmailService();

    // ==================== ĐĂNG NHẬP ====================

    /** Kiểm tra đăng nhập. Đúng thì trả về User, sai thì ném lỗi. */
    public User login(String email, String password) throws IllegalArgumentException {
        // Bước 1: kiểm tra dữ liệu nhập
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Email!");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống!");
        }

        // Bước 2: tìm tài khoản
        User user = userDao.findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Tài khoản chưa được đăng ký!");
        }

        // Bước 3: kiểm tra mật khẩu (làm trước bước kiểm tra trạng thái
        // để người không biết mật khẩu không dò được tài khoản nào đang bị khóa)
        if (!PasswordUtil.verify(password.trim(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng. Vui lòng thử lại!");
        }

        // Bước 4: chỉ tài khoản Active mới được đăng nhập
        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa hoặc vô hiệu hóa. Vui lòng liên hệ hỗ trợ!");
        }
        return user;
    }

    // ==================== ĐĂNG KÝ (2 BƯỚC) ====================

    /**
     * Bước 1 đăng ký: kiểm tra dữ liệu và gửi OTP qua email. CHƯA tạo tài khoản.
     * Trả về thông tin đăng ký tạm, servlet giữ trong session để xác thực OTP ở bước 2.
     */
    public PendingRegistration register(String fullName, String email, String phone,
            String password, String confirmPassword) throws IllegalArgumentException {

        // Bước 1: kiểm tra từng trường
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }
        if (fullName.trim().length() > 150) {
            throw new IllegalArgumentException("Họ và tên quá dài (tối đa 150 ký tự)!");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }
        if (!Validator.isValidEmail(email)) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ!");
        }
        if (email.trim().length() > 100) {
            throw new IllegalArgumentException("Email quá dài (tối đa 100 ký tự)!");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống!");
        }
        if (!Validator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng 0)!");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống!");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải chứa ít nhất 6 ký tự!");
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        String emailNormal = email.trim().toLowerCase();

        // Bước 2: kiểm tra trùng email, trùng số điện thoại
        if (userDao.findByEmail(emailNormal) != null) {
            throw new IllegalArgumentException("Email này đã được sử dụng. Vui lòng chọn email khác!");
        }
        if (userDao.findByPhone(phone.trim()) != null) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng. Vui lòng chọn số khác!");
        }

        // Bước 3: không cho gửi OTP quá nhanh
        long waitSeconds = OtpGuard.resendWaitSeconds(emailNormal);
        if (waitSeconds > 0) {
            throw new IllegalArgumentException("Vui lòng đợi " + waitSeconds + " giây trước khi yêu cầu mã OTP mới!");
        }

        // Bước 4: gửi OTP, rồi tạo thông tin đăng ký tạm (mật khẩu đã mã hóa)
        sendOtp(emailNormal);
        return new PendingRegistration(fullName.trim(), emailNormal, phone.trim(), PasswordUtil.hash(password));
    }

    /** Gửi lại OTP cho đăng ký đang chờ xác thực. */
    public void resendRegistrationOtp(PendingRegistration pending) throws IllegalArgumentException {
        if (pending == null) {
            throw new IllegalArgumentException("Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại!");
        }
        String email = pending.getEmail();

        // Email đã có tài khoản thì không cần gửi nữa
        if (userDao.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email này đã được đăng ký. Vui lòng đăng nhập!");
        }

        // Không cho gửi quá nhanh
        long waitSeconds = OtpGuard.resendWaitSeconds(email);
        if (waitSeconds > 0) {
            throw new IllegalArgumentException("Vui lòng đợi " + waitSeconds + " giây trước khi gửi lại mã OTP!");
        }

        sendOtp(email);
    }

    /**
     * Bước 2 đăng ký: nhập đúng OTP thì mới tạo tài khoản (vai trò Customer, trạng thái Active).
     * Tài khoản chỉ được tạo ở bước này nên OTP không thể dùng để mở lại tài khoản đã bị khóa.
     */
    public void completeRegistration(PendingRegistration pending, String otpCode) throws IllegalArgumentException {
        // Bước 1: kiểm tra dữ liệu
        if (pending == null) {
            throw new IllegalArgumentException("Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại!");
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã xác thực OTP!");
        }
        String email = pending.getEmail();
        otpCode = otpCode.trim();
        if (!otpCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Mã OTP gồm đúng 6 chữ số!");
        }

        // Bước 2: kiểm tra có đang bị khóa vì nhập sai nhiều lần không
        long lockSeconds = OtpGuard.lockRemainingSeconds(email);
        if (lockSeconds > 0) {
            long lockMinutes = (lockSeconds + 59) / 60;
            throw new IllegalArgumentException("Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau "
                    + lockMinutes + " phút hoặc bấm gửi lại mã OTP mới!");
        }

        // Bước 3: kiểm tra trùng lại (có thể người khác đã đăng ký trong lúc đang chờ)
        if (userDao.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email này đã được đăng ký. Vui lòng đăng nhập!");
        }
        if (userDao.findByPhone(pending.getPhone()) != null) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng. Vui lòng đăng ký lại với số khác!");
        }

        // Bước 4: kiểm tra OTP
        if (!userDao.verifyOtp(email, otpCode)) {
            int attemptsLeft = OtpGuard.recordFailure(email);
            if (attemptsLeft > 0) {
                throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn (" + OTP_EXPIRY_MINUTES
                        + " phút)! Bạn còn " + attemptsLeft + " lần thử.");
            }
            throw new IllegalArgumentException("Bạn đã nhập sai quá " + OtpGuard.MAX_ATTEMPTS
                    + " lần. Vui lòng bấm gửi lại mã OTP mới!");
        }
        OtpGuard.reset(email);

        // Bước 5: OTP đúng, tạo tài khoản
        User newUser = new User(0, pending.getFullName(), email, pending.getPhone(),
                pending.getPasswordHash(), "Customer", "Active");
        if (!userDao.insertUser(newUser)) {
            throw new IllegalArgumentException("Không thể tạo tài khoản. Vui lòng đăng ký lại!");
        }
    }

    /** Tạo OTP mới, lưu vào database và gửi qua email. */
    private void sendOtp(String email) throws IllegalArgumentException {
        String otpCode = OtpUtil.generate6DigitOtp();

        if (!userDao.saveOtp(email, otpCode, OTP_EXPIRY_MINUTES)) {
            throw new IllegalArgumentException("Không thể lưu mã OTP. Vui lòng thử lại!");
        }
        if (!emailService.sendOtpEmail(email, otpCode)) {
            throw new IllegalArgumentException("Không thể gửi email OTP. Vui lòng kiểm tra địa chỉ email và thử lại!");
        }

        OtpGuard.markSent(email);
        OtpGuard.reset(email);
    }

    // ==================== QUÊN MẬT KHẨU ====================

    /** Tạo mật khẩu mới ngẫu nhiên, lưu vào database và gửi qua email. */
    public void forgotPassword(String email) throws IllegalArgumentException {
        // Bước 1: kiểm tra email và tìm tài khoản
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email.");
        }
        User user = userDao.findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống.");
        }

        // Bước 2: tạo mật khẩu mới và lưu (đã mã hóa) vào database
        String newPassword = OtpUtil.generatePassword();
        if (!userDao.changePassword(user.getUserID(), PasswordUtil.hash(newPassword))) {
            throw new IllegalArgumentException("Không thể cập nhật mật khẩu. Vui lòng thử lại.");
        }

        // Bước 3: gửi mật khẩu mới qua email
        if (emailService.sendPasswordEmail(user.getEmail(), newPassword)) {
            return;
        }

        // Bước 4: gửi email thất bại thì khôi phục mật khẩu cũ
        if (userDao.changePassword(user.getUserID(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Không thể gửi email. Mật khẩu cũ vẫn được giữ nguyên.");
        }
        throw new IllegalArgumentException("Gửi email thất bại và không thể khôi phục mật khẩu. "
                + "Vui lòng liên hệ quản trị viên.");
    }

    // ==================== HỒ SƠ & ĐỔI MẬT KHẨU ====================

    /** Cập nhật họ tên, số điện thoại. Trả về User đã cập nhật. */
    public User updateProfile(int userId, String fullName, String phone) throws IllegalArgumentException {
        // Bước 1: kiểm tra dữ liệu
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống!");
        }
        if (!Validator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ!");
        }

        // Bước 2: số điện thoại không được trùng với tài khoản khác
        User sameUserPhone = userDao.findByPhone(phone.trim());
        if (sameUserPhone != null && sameUserPhone.getUserID() != userId) {
            throw new IllegalArgumentException("Số điện thoại này đã được tài khoản khác sử dụng!");
        }

        // Bước 3: cập nhật
        if (!userDao.updateProfile(userId, fullName.trim(), phone.trim())) {
            throw new IllegalArgumentException("Cập nhật thất bại. Vui lòng thử lại!");
        }
        return userDao.findById(userId);
    }

    /** Đổi mật khẩu. */
    public void changePassword(int userId, String oldPassword, String newPassword, String confirmPassword)
            throws IllegalArgumentException {
        // Bước 1: kiểm tra dữ liệu
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại!");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới!");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        // Bước 2: kiểm tra mật khẩu cũ
        User user = userDao.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Tài khoản không tồn tại!");
        }
        if (!PasswordUtil.verify(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }

        // Bước 3: lưu mật khẩu mới (đã mã hóa)
        if (!userDao.changePassword(userId, PasswordUtil.hash(newPassword))) {
            throw new IllegalArgumentException("Đổi mật khẩu thất bại. Vui lòng thử lại!");
        }
    }

    // ==================== THÔNG TIN ĐĂNG KÝ TẠM ====================

    /**
     * Thông tin đăng ký đang chờ xác thực OTP, được giữ trong session.
     * Tài khoản chỉ được tạo trong bảng [User] SAU KHI nhập đúng OTP.
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
