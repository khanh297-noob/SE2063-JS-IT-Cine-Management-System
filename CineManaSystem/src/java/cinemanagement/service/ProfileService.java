package cinemanagement.service;

import java.sql.SQLException;

import cinemanagement.dao.AuditLogDAO;
import cinemanagement.dao.BookingDAO;
import cinemanagement.dao.SeatHoldDAO;
import cinemanagement.dao.UserDAO;
import cinemanagement.model.User;
import cinemanagement.util.BusinessException;
import cinemanagement.util.PasswordHasher;

public class ProfileService {
    private final UserDAO userDAO = new UserDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final SeatHoldDAO holdDAO = new SeatHoldDAO();
    private final AuditLogDAO audit = new AuditLogDAO();

    public User load(int userId) throws SQLException {
        return userDAO.findById(userId);
    }

    public void updateProfile(User actor, String fullName, String phone, String ip) throws SQLException, BusinessException {
        String name = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        String ph = phone == null ? "" : phone.trim();
        if (name.isEmpty()) {
            throw new BusinessException("Họ tên là bắt buộc.");
        }
        if (name.length() > 100) {
            throw new BusinessException("Họ tên vượt quá độ dài tối đa 100 ký tự.");
        }
        if (!ph.matches("\\d{9,11}")) {
            throw new BusinessException("Số điện thoại không hợp lệ (9-11 chữ số).");
        }
        userDAO.updateProfile(actor.getUserId(), name, ph);
        actor.setFullName(name);
        actor.setPhone(ph);
        audit.log(actor.getUserId(), "Update profile", ip);
    }

    public void changePassword(User actor, String current, String next, String confirm, String ip)
            throws SQLException, BusinessException {
        User u = userDAO.findById(actor.getUserId());
        if (u == null || !PasswordHasher.verify(current, u.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }
        String err = PasswordHasher.validateStrength(next);
        if (err != null) {
            throw new BusinessException(err);
        }
        if (!next.equals(confirm)) {
            throw new BusinessException("Xác nhận mật khẩu không khớp.");
        }
        if (next.equals(current)) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        userDAO.updatePassword(u.getUserId(), PasswordHasher.hash(next));
        audit.log(u.getUserId(), "Change password", ip);
    }

    public void deactivate(User actor, String password, boolean confirmed, String ip) throws SQLException, BusinessException {
        if (!confirmed) {
            throw new BusinessException("Vui lòng tích xác nhận bạn hiểu hậu quả của việc vô hiệu hóa tài khoản.");
        }
        User u = userDAO.findById(actor.getUserId());
        if (u == null || !PasswordHasher.verify(password, u.getPasswordHash())) {
            throw new BusinessException("Mật khẩu không đúng.");
        }
        int upcoming = bookingDAO.countUpcomingConfirmed(u.getUserId());
        if (upcoming > 0) {
            throw new BusinessException("Bạn còn " + upcoming + " đơn vé sắp chiếu. Hãy hủy hoặc sử dụng hết trước khi vô hiệu hóa tài khoản.");
        }
        holdDAO.deleteByUser(u.getUserId());
        userDAO.updateStatus(u.getUserId(), "Deactivated");
        audit.log(u.getUserId(), "Self-deactivate account", ip);
    }
}
