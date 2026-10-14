package cinemanagement.controller;

import cinemanagement.dao.UserDAO;
import cinemanagement.model.User;
import cinemanagement.util.PasswordHasher;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Dang nhap TOI GIAN de test cac chuc nang (module Authentication thuoc phan cua ban khac).
 * Khi ghep code, chi can dam bao session attribute "user" chua doi tuong cinemanagement.model.User.
 */
@WebServlet("/login")
public class LoginServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (Web.currentUser(req) != null) {
            Web.redirect(req, resp, "/");
            return;
        }
        Web.forward(req, resp, "login");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String email = Web.trim(req.getParameter("email"));
        String password = req.getParameter("password");
        String next = req.getParameter("next");
        User u = email.isEmpty() ? null : userDAO.findByEmail(email);
        if (u == null || !PasswordHasher.verify(password, u.getPasswordHash())) {
            req.setAttribute("error", "Sai email hoặc mật khẩu. Vui lòng kiểm tra lại. (MSG08)");
            req.setAttribute("email", email);
            Web.forward(req, resp, "login");
            return;
        }
        if (!u.isActive()) {
            req.setAttribute("error", "Tài khoản hiện không khả dụng. Vui lòng liên hệ quản trị viên. (MSG09)");
            req.setAttribute("email", email);
            Web.forward(req, resp, "login");
            return;
        }
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();   // chong session fixation
        }
        HttpSession s = req.getSession(true);
        s.setAttribute(Web.SESSION_USER, u.sessionCopy());
        Web.flash(req, "success", "Đăng nhập thành công. (MSG10)");

        if (next != null && next.startsWith("/") && !next.startsWith("//")) {
            Web.redirect(req, resp, next);
        } else if (u.isManager() || u.isAdmin()) {
            Web.redirect(req, resp, "/manager/showtimes");
        } else if (u.isCashier()) {
            Web.redirect(req, resp, "/pos");
        } else {
            Web.redirect(req, resp, "/");
        }
    }
}
