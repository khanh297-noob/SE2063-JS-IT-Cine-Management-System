package controller.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import model.User;
import service.AuthService;
import util.PermissionUtil;
import util.SessionUtil;

/**
 * Đăng nhập: GET hiện form, POST kiểm tra tài khoản rồi chuyển sang trang phù hợp với vai trò.
 */
@WebServlet(name = "login", urlPatterns = {"/login"})
public class Login extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/view/authentication/login-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            // Bước 1: kiểm tra tài khoản, sai thì service ném IllegalArgumentException
            User user = authService.login(email, password);

            // Bước 2: lưu user vào session
            SessionUtil.login(request, user);

            // Bước 3: chuyển sang trang phù hợp
            redirectAfterLogin(request, response);

        } catch (IllegalArgumentException e) {
            request.setAttribute("email", email);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/view/authentication/login-tuyet.jsp").forward(request, response);
        }
    }

    /** Quyết định đăng nhập xong thì đi đâu. */
    private void redirectAfterLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String contextPath = request.getContextPath();

        // Khách hàng (hoặc người không thuộc nội bộ): luôn về trang chủ
        if (PermissionUtil.isCustomer(request) || !PermissionUtil.isInternalUser(request)) {
            request.getSession().removeAttribute("redirectAfterLogin");
            response.sendRedirect(contextPath + "/index-tuyet.jsp");
            return;
        }

        // Nhân sự nội bộ: quay lại trang trước đó bị AuthFilter chặn (nếu có)
        String redirectUrl = (String) request.getSession().getAttribute("redirectAfterLogin");
        if (redirectUrl != null) {
            request.getSession().removeAttribute("redirectAfterLogin");
            response.sendRedirect(redirectUrl);
            return;
        }

        // Không có trang trước đó: Admin/Quản lý vào dashboard, còn lại về trang chủ
        if (PermissionUtil.isBranchManager(request)) {
            response.sendRedirect(contextPath + "/branch-home.jsp");
        }
        else if(PermissionUtil.isAdmin(request)){
            response.sendRedirect(contextPath + "/admin-home.jsp");
        }
        // Thu ngân vào ca bán vé tại quầy, nhân viên soát vé vào trang soát vé
        else if (PermissionUtil.hasRole(request, "Ticket Cashier")) {
            response.sendRedirect(contextPath + "/pos/shift");
        }
        else if (PermissionUtil.hasRole(request, "Ticket Checker")) {
            response.sendRedirect(contextPath + "/checker");
        }else {
            response.sendRedirect(contextPath + "/index-tuyet.jsp");
        }
    }
}
