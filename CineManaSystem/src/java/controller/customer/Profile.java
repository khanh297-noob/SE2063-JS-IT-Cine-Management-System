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
import util.SessionUtil;

@WebServlet(name = "customerProfile", urlPatterns = {"/customer/profile"})
public class Profile extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = PermissionUtil.getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute("user", currentUser);
        request.getRequestDispatcher("/view/customer/profile.jsp").forward(request, response);
    }

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

        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");

        AuthService.AuthResult result = authService.updateProfile(currentUser.getUserID(), fullName, phone);

        if (result.success) {
            SessionUtil.login(request, result.user);
            request.setAttribute("user", result.user);
            request.setAttribute("successMessage", "Thông tin tài khoản đã được cập nhật thành công!");
        } else {
            request.setAttribute("user", currentUser);
            request.setAttribute("errorMessage", result.message);
        }

        request.getRequestDispatcher("/view/customer/profile.jsp").forward(request, response);
    }
}
