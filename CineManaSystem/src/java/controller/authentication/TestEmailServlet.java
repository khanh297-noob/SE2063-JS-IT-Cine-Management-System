package controller.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import thirdparty.EmailService;

/**
 * Trang thử gửi email (chỉ Admin dùng được, AuthFilter đã chặn). Nên xóa trước khi triển khai.
 */
@WebServlet(name = "testEmail", urlPatterns = {"/test-email"})
public class TestEmailServlet extends HttpServlet {

    private EmailService emailService;

    @Override
    public void init() throws ServletException {
        emailService = new EmailService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        // Thay bằng email bạn muốn nhận thư thử
        String receiverEmail = "tuyetphan270106@gmail.com";

        if (emailService.sendOtpEmail(receiverEmail, "123456")) {
            response.getWriter().println("Gui email thanh cong! Hay kiem tra hop thu.");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Gui email that bai! Hay kiem tra Output cua NetBeans.");
        }
    }
}
