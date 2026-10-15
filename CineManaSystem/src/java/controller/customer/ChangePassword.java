package controller.customer;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import service.AuthService;
import util.PermissionUtil;

@WebServlet(name = "customerChangePassword", urlPatterns = {"/customer/change-password"})
public class ChangePassword extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        User currentUser = PermissionUtil.getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        AuthService.AuthResult result = authService.changePassword(
                currentUser.getUserID(), oldPassword, newPassword, confirmPassword);

        if (result.success) {
            request.setAttribute("passwordSuccess", "Đổi mật khẩu thành công!");
        } else {
            request.setAttribute("passwordError", result.message);
        }

        request.setAttribute("user", currentUser);
        request.getRequestDispatcher("/view/customer/profile.jsp").forward(request, response);
    }
}
