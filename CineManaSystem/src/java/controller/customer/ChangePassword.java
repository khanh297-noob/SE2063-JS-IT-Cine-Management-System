package controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import model.User;
import service.AuthService;
import util.PermissionUtil;

/**
 * Đổi mật khẩu (form nằm trong trang hồ sơ cá nhân).
 */
@WebServlet(name = "customerChangePassword", urlPatterns = {"/customer/change-password"})
public class ChangePassword extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = PermissionUtil.getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
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
        request.getRequestDispatcher("/WEB-INF/view/customer/profile-tuyet.jsp").forward(request, response);
    }
}
