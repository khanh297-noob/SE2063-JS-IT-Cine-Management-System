package controller.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import service.AuthService;
import service.AuthService.PendingRegistration;

/**
 * Đăng ký bước 1: kiểm tra dữ liệu, gửi OTP qua email rồi chuyển sang trang nhập OTP.
 * Tài khoản CHƯA được tạo ở bước này.
 */
@WebServlet(name = "register", urlPatterns = {"/register"})
public class Register extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/authentication/register-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            // Bước 1: kiểm tra dữ liệu và gửi OTP
            PendingRegistration pending = authService.register(fullName, email, phone, password, confirmPassword);

            // Bước 2: giữ thông tin đăng ký trong session cho tới khi nhập đúng OTP
            request.getSession().setAttribute("pendingRegistration", pending);
            request.getSession().setAttribute("verifyEmail", pending.getEmail());
            request.getSession().setAttribute("registerSuccess",
                    "Mã OTP đã được gửi đến email của bạn. Vui lòng nhập mã để hoàn tất đăng ký.");

            // Bước 3: chuyển sang trang nhập OTP
            response.sendRedirect(request.getContextPath() + "/verify-email");

        } catch (IllegalArgumentException e) {
            // Giữ lại dữ liệu đã nhập để người dùng không phải gõ lại
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.setAttribute("phone", phone);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/view/authentication/register-tuyet.jsp").forward(request, response);
        }
    }
}
