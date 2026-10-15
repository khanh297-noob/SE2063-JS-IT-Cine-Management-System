package controller.authentication;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import service.AuthService;
import util.PermissionUtil;
import util.SessionUtil;

@WebServlet(name = "login", urlPatterns = {"/login"})
public class Login extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/view/authentication/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        AuthService.AuthResult result = authService.login(email, password);

        if (result.success) {
            SessionUtil.login(request, result.user);

            // 1. Nếu là Customer -> luôn điều hướng đến trang chủ index.jsp
            if (PermissionUtil.isCustomer(request) || !PermissionUtil.isInternalUser(request)) {
                request.getSession().removeAttribute("redirectAfterLogin");
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                return;
            }

            // 2. Với nhân viên nội bộ (Admin/Manager/Staff), kiểm tra nếu có trang trước đó bị chặn bởi AuthFilter
            String redirectUrl = (String) request.getSession().getAttribute("redirectAfterLogin");
            if (redirectUrl != null) {
                request.getSession().removeAttribute("redirectAfterLogin");
                response.sendRedirect(redirectUrl);
                return;
            }

            // 3. Điều hướng mặc định cho nhân viên quản lý/báo cáo
            if (PermissionUtil.canViewSalesReport(request)) {
                response.sendRedirect(request.getContextPath() + "/report/branch-dashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } else {
            request.setAttribute("email", email);
            request.setAttribute("error", result.message);

            request.getRequestDispatcher("/view/authentication/login.jsp").forward(request, response);
        }
    }
}
