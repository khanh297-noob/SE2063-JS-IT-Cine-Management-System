
package controller.authentication;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.EmailService;

@WebServlet(name = "testEmail", urlPatterns = {"/test-email"})
public class TestEmailServlet extends HttpServlet {

    private final EmailService emailService = new EmailService();

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        // Thay bằng email bạn muốn nhận thư thử
        String receiverEmail = "tuyetphan270106@gmail.com";

        boolean success = emailService.sendOtpEmail(
                receiverEmail, "123456"
        );

        if (success) {
            response.getWriter().println(
                    "Gui email thanh cong! Hay kiem tra hop thu."
            );
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(
                    "Gui email that bai! Hay kiem tra Output cua NetBeans."
            );
        }
    }
}
