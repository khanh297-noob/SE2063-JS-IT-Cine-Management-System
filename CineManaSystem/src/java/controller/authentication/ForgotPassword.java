/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.authentication;

import dao.UserDao;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import util.PasswordUtil;
import service.EmailService;
import util.OtpUtil;

/**
 *
 * @author adm
 */
@WebServlet(name = "ForgotPassword", urlPatterns = { "/forgot-password" })
public class ForgotPassword extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request,
                        HttpServletResponse response)
                        throws ServletException, IOException {

                request.getRequestDispatcher("/view/authentication/forgot-password.jsp")
                                .forward(request, response);
        }

        @Override
        protected void doPost(HttpServletRequest request,
                        HttpServletResponse response)
                        throws ServletException, IOException {

                request.setCharacterEncoding("UTF-8");
                response.setContentType("text/html;charset=UTF-8");

                String email = request.getParameter("email");

                if (email == null || email.trim().isEmpty()) {
                        request.setAttribute("error", "Vui lòng nhập email.");
                        request.getRequestDispatcher("/view/authentication/forgot-password.jsp")
                                        .forward(request, response);
                        return;
                }

                email = email.trim();

                UserDao userDao = new UserDao();

                try {
                        // 1. Tìm tài khoản theo email
                        User user = userDao.findByEmail(email);

                        if (user == null) {
                                request.setAttribute(
                                                "error",
                                                "Email không tồn tại trong hệ thống.");
                                request.setAttribute("email", email);
                                request.getRequestDispatcher("/view/authentication/forgot-password.jsp")
                                                .forward(request, response);
                                return;
                        }

                        // 2. Tạo mật khẩu mới gồm 6 ký tự
                        String newPassword = OtpUtil.generatePassword();

                        // 3. Mã hóa mật khẩu bằng PasswordUtil
                        String newPasswordHash = PasswordUtil.hash(newPassword);

                        // 4. Cập nhật mật khẩu mới vào database
                        boolean updated = userDao.changePassword(
                                        user.getUserID(),
                                        newPasswordHash);

                        if (!updated) {
                                request.setAttribute(
                                                "error",
                                                "Không thể cập nhật mật khẩu. Vui lòng thử lại.");
                                request.setAttribute("email", email);
                                request.getRequestDispatcher("/view/authentication/forgot-password.jsp")
                                                .forward(request, response);
                                return;
                        }

                        // 5. Gửi mật khẩu mới qua email
                        EmailService emailService = new EmailService();

                        boolean sent = emailService.sendPasswordEmail(
                                        email,
                                        newPassword);

                        if (sent) {
                                request.getSession().setAttribute("message",
                                                "Mật khẩu mới đã được gửi đến email của bạn.");
                                response.sendRedirect(request.getContextPath() + "/login");
                                return;
                        } else {
                                // Khôi phục mật khẩu cũ nếu gửi email thất bại
                                boolean restored = userDao.changePassword(
                                                user.getUserID(),
                                                user.getPasswordHash());

                                if (restored) {
                                        request.setAttribute(
                                                        "error",
                                                        "Không thể gửi email. Mật khẩu cũ vẫn được giữ nguyên.");
                                } else {
                                        request.setAttribute(
                                                        "error",
                                                        "Gửi email thất bại và không thể khôi phục mật khẩu. "
                                                                        + "Vui lòng liên hệ quản trị viên.");
                                }
                        }

                } catch (Exception e) {
                        log("ForgotPasswordServlet error", e);
                        request.setAttribute(
                                        "error",
                                        "Đã xảy ra lỗi. Vui lòng thử lại sau.");
                } finally {
                        userDao.close();
                }

                request.setAttribute("email", email);
                request.getRequestDispatcher("/view/authentication/forgot-password.jsp")
                                .forward(request, response);
        }

}
