package controller.authentication;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService.PendingRegistration;
import service.AuthService;

@WebServlet(name = "resendOtp", urlPatterns = {"/resend-otp"})
public class ResendOtp extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PendingRegistration pending =
                (PendingRegistration) request.getSession().getAttribute("pendingRegistration");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        AuthService.AuthResult result = authService.resendRegistrationOtp(pending);
        if (result.success) {
            request.getSession().setAttribute("resendMessage", "Mã OTP mới đã được gửi lại!");
        } else {
            request.getSession().setAttribute("resendError", result.message);
        }
        response.sendRedirect(request.getContextPath() + "/verify-email");
    }
}
