package cinemanagement.controller;

import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        Web.flash(req, "success", "Đăng xuất thành công. (MSG11)");
        Web.redirect(req, resp, "/");
    }
}
