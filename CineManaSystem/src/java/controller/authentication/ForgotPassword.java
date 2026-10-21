package controller.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import service.AuthService;

/**
 * Quên mật khẩu: tạo mật khẩu mới và gửi về email.
 */
@WebServlet(name = "ForgotPassword", urlPatterns = {"/forgot-password"})
public class ForgotPassword extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/authentication/forgot-password-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");

        try {
            authService.forgotPassword(email);

            // Thành công: báo ở trang đăng nhập
            request.getSession().setAttribute("message", "Mật khẩu mới đã được gửi đến email của bạn.");
            response.sendRedirect(request.getContextPath() + "/login");

        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/view/authentication/forgot-password-tuyet.jsp").forward(request, response);

        } catch (Exception e) {
            log("ForgotPassword error", e);
            request.setAttribute("error", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/view/authentication/forgot-password-tuyet.jsp").forward(request, response);
        }
    }
}
