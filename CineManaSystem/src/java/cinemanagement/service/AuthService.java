package cinemanagement.service;

import java.sql.SQLException;

import cinemanagement.dao.UserDAO;
import cinemanagement.model.User;
import cinemanagement.util.BusinessException;
import cinemanagement.util.PasswordHasher;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User login(String email, String password) throws SQLException, BusinessException {
        User u = (email == null || email.isEmpty()) ? null : userDAO.findByEmail(email);
        if (u == null || !PasswordHasher.verify(password, u.getPasswordHash())) {
            throw new BusinessException("Sai email hoặc mật khẩu. Vui lòng kiểm tra lại. (MSG08)");
        }
        if (!u.isActive()) {
            throw new BusinessException("Tài khoản hiện không khả dụng. Vui lòng liên hệ quản trị viên. (MSG09)");
        }
        return u;
    }

    public String homePathFor(User u) {
        if (u.isManager() || u.isAdmin()) {
            return "/manager";
        }
        return "/home";
    }
}
