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
 * Gửi lại mã OTP cho đăng ký đang chờ xác thực.
 */
@WebServlet(name = "resendOtp", urlPatterns = {"/resend-otp"})
public class ResendOtp extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PendingRegistration pending = (PendingRegistration) request.getSession().getAttribute("pendingRegistration");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        try {
            authService.resendRegistrationOtp(pending);
            request.getSession().setAttribute("resendMessage", "Mã OTP mới đã được gửi lại!");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("resendError", e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/verify-email");
    }
}
