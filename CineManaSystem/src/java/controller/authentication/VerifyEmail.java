package controller.authentication;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService.PendingRegistration;
import service.AuthService;

@WebServlet(name = "verifyEmail", urlPatterns = {"/verify-email"})
public class VerifyEmail extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PendingRegistration pending =
                (PendingRegistration) request.getSession().getAttribute("pendingRegistration");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        request.setAttribute("email", pending.getEmail());
        request.getRequestDispatcher("/view/authentication/verify-email.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        PendingRegistration pending =
                (PendingRegistration) request.getSession().getAttribute("pendingRegistration");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        // Email lấy từ session, thông tin dữ liệu form
        String otpCode = request.getParameter("otpCode");
        AuthService.AuthResult result = authService.completeRegistration(pending, otpCode);

        if (result.success) {
            request.getSession().removeAttribute("pendingRegistration");
            request.getSession().removeAttribute("verifyEmail");
            request.getSession().removeAttribute("registerSuccess");
            request.getSession().setAttribute("verifySuccess", "Tài khoản của bạn đã được xác thực thành công! Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + "/login");
        } else {
            request.setAttribute("email", pending.getEmail());
            request.setAttribute("error", result.message);
            request.getRequestDispatcher("/view/authentication/verify-email.jsp")
                   .forward(request, response);
        }
    }
}
