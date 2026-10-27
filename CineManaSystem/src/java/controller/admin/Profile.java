package controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import model.User;
import service.AuthService;
import util.PermissionUtil;
import util.SessionUtil;

/**
 * Xem và cập nhật hồ sơ cá nhân dành riêng cho Admin và Manager.
 */
@WebServlet(name = "Profile", urlPatterns = {"/admin/profile"})
public class Profile extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = PermissionUtil.getCurrentUser(request);

        // 1. Kiểm tra đăng nhập
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

        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");

        try {
            // Cập nhật thông tin và làm mới Session
            User updatedUser = authService.updateProfile(currentUser.getUserID(), fullName, phone);
            SessionUtil.login(request, updatedUser);

            request.setAttribute("user", updatedUser);
            request.setAttribute("successMessage", "Cập nhật hồ sơ quản trị viên thành công!");

        } catch (IllegalArgumentException e) {
            request.setAttribute("user", currentUser);
            request.setAttribute("errorMessage", e.getMessage());
        }

        request.getRequestDispatcher("/WEB-INF/view/admin/profile-changepassword-admin.jsp").forward(request, response);
    }

    /**
     * Helper kiểm tra Vai trò có thuộc nhóm Quản trị viên / Quản lý không
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