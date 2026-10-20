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
 * Đăng ký bước 2: nhập OTP. Đúng OTP thì tạo tài khoản rồi chuyển sang trang đăng nhập.
 */
@WebServlet(name = "verifyEmail", urlPatterns = {"/verify-email"})
public class VerifyEmail extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Chưa đăng ký thì không có trang nhập OTP, quay lại trang đăng ký
        PendingRegistration pending = (PendingRegistration) request.getSession().getAttribute("pendingRegistration");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        request.setAttribute("email", pending.getEmail());
        request.getRequestDispatcher("/WEB-INF/view/authentication/verify-email-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PendingRegistration pending = (PendingRegistration) request.getSession().getAttribute("pendingRegistration");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        String otpCode = request.getParameter("otpCode");

        try {
            // Bước 1: kiểm tra OTP và tạo tài khoản
            authService.completeRegistration(pending, otpCode);

            // Bước 2: xóa thông tin đăng ký tạm trong session
            request.getSession().removeAttribute("pendingRegistration");
            request.getSession().removeAttribute("verifyEmail");
            request.getSession().removeAttribute("registerSuccess");

            // Bước 3: báo thành công và chuyển sang trang đăng nhập
            request.getSession().setAttribute("verifySuccess",
                    "Tài khoản của bạn đã được xác thực thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + "/login");

        } catch (IllegalArgumentException e) {
            request.setAttribute("email", pending.getEmail());
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/view/authentication/verify-email-tuyet.jsp").forward(request, response);
        }
    }
}
