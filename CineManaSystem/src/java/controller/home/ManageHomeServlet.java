package controller.home;

import controller.BaseServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import util.PermissionUtil;

/**
 * Lối vào trang quản trị cũ. Trang chủ quản trị nay theo dự án chung:
 * GET /manage  Admin -> /admin-home.jsp, Quản lý chi nhánh -> /branch-home.jsp, còn lại -> /home
 */
@WebServlet("/manage")
public class ManageHomeServlet extends BaseServlet {

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (PermissionUtil.isAdmin(req)) {
            redirect(req, resp, "/admin-home.jsp");
        } else if (PermissionUtil.isBranchManager(req)) {
            redirect(req, resp, "/branch-home.jsp");
        } else {
            redirect(req, resp, "/home");
        }
    }
}
