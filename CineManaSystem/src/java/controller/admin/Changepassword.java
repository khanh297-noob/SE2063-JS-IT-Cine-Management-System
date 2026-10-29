package controller.admin;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import service.AuthService;
import util.PermissionUtil;

/**
 * Đổi mật khẩu dành riêng cho Admin và Manager.
 */
@WebServlet(name = "Changepassword", urlPatterns = {"/admin/change-password", "/changepassword"})
public class Changepassword extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = PermissionUtil.getCurrentUser(request);

        // 1. Kiểm tra chưa đăng nhập
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 2. Kiểm tra quyền Admin/Manager
        if (!isAdminOrManager(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
            return;
        }

        request.setAttribute("user", currentUser);
        request.getRequestDispatcher("/WEB-INF/view/admin/profile-changepassword-admin.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = PermissionUtil.getCurrentUser(request);

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!isAdminOrManager(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            authService.changePassword(currentUser.getUserID(), oldPassword, newPassword, confirmPassword);
            request.setAttribute("passwordSuccess", "Đổi mật khẩu thành công!");
        } catch (IllegalArgumentException e) {
            request.setAttribute("passwordError", e.getMessage());
        }

        request.setAttribute("user", currentUser);
        request.getRequestDispatcher("/WEB-INF/view/admin/profile-changepassword-admin.jsp").forward(request, response);
    }

    /**
     * Helper kiểm tra Vai trò có thuộc nhóm Quản trị viên / Quản lý rạp không
     */
    private boolean isAdminOrManager(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole();
        return role.equalsIgnoreCase("Administrator") ||
               role.equalsIgnoreCase("Admin") ||
               role.equalsIgnoreCase("Branch Management") ||
               role.equalsIgnoreCase("BranchManager");
    }
}
