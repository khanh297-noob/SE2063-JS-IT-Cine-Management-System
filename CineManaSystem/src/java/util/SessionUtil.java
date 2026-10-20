package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.User;

/**
 * Lưu, lấy và xóa thông tin người dùng đang đăng nhập trong session.
 */
public class SessionUtil {

    // Tên key lưu user trong session
    private static final String SESSION_USER_KEY = "User";

    // Session hết hạn sau 30 phút không hoạt động
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    /** Lưu user vào session (dùng khi đăng nhập hoặc làm mới thông tin user). */
    public static void login(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
    }

    /** Trả về user đang đăng nhập, hoặc null nếu chưa đăng nhập. */
    public static User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object obj = session.getAttribute(SESSION_USER_KEY);
        if (obj instanceof User) {
            return (User) obj;
        }
        return null;
    }

    /** Hủy session, đăng xuất người dùng. */
    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
