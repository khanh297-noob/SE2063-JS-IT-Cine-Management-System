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
import util.SessionUtil;

/**
 * Xem và cập nhật hồ sơ cá nhân (họ tên, số điện thoại).
 */
@WebServlet(name = "customerProfile", urlPatterns = {"/customer/profile"})
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
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute("user", currentUser);
        request.getRequestDispatcher("/WEB-INF/view/customer/profile-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = PermissionUtil.getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");

        try {
            // Cập nhật thành công thì làm mới user trong session
            User updatedUser = authService.updateProfile(currentUser.getUserID(), fullName, phone);
            SessionUtil.login(request, updatedUser);

            request.setAttribute("user", updatedUser);
            request.setAttribute("successMessage", "Thông tin tài khoản đã được cập nhật thành công!");

        } catch (IllegalArgumentException e) {
            request.setAttribute("user", currentUser);
            request.setAttribute("errorMessage", e.getMessage());
        }

        request.getRequestDispatcher("/WEB-INF/view/customer/profile-tuyet.jsp").forward(request, response);
    }
}
