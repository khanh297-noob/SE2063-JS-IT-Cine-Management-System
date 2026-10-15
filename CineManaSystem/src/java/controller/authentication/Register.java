package controller.authentication;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;

@WebServlet(name = "register", urlPatterns = {"/register"})
public class Register extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/view/authentication/register.jsp")
           .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        AuthService.AuthResult result =
                authService.register(fullName, email, phone, password, confirmPassword);

        if (result.success) {
            // Tài khoản CHƯA được tạo; giữ thông tin trong session cho đến khi nhập đúng OTP
            request.getSession().setAttribute("pendingRegistration", result.pending);
            request.getSession().setAttribute("verifyEmail", result.pending.getEmail());
            request.getSession().setAttribute("registerSuccess", result.message);
            response.sendRedirect(request.getContextPath() + "/verify-email");
        } else {
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.setAttribute("phone", phone);
            request.setAttribute("error", result.message);

            request.getRequestDispatcher("/view/authentication/register.jsp")
                   .forward(request, response);
        }
    }
}
