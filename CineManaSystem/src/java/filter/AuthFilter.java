package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;

/**
 * Kiểm tra (1) đã đăng nhập chưa, (2) có đúng vai trò không, cho các trang phần 6 & 7.
 */
// Webhook /api/v1/payment/webhook/* KHÔNG nằm trong danh sách vì do máy chủ VNPay/MoMo gọi (không đăng nhập)
@WebFilter(urlPatterns = {"/payment/*", "/api/v1/payment/create", "/admin/*", "/manager/*", "/pos", "/pos/*", "/checker"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // VNPay / MoMo chuyển trình duyệt về 2 link này (có thể mất session) - đã kiểm tra chữ ký trong Servlet
        if (path.startsWith("/payment/vnpay-return") || path.startsWith("/payment/momo-return")) {
            chain.doFilter(request, response);
            return;
        }

        // 1. Chưa đăng nhập -> về trang đăng nhập
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "Your session has expired. Please log in again.");
            res.sendRedirect(req.getContextPath() + "/index.html");
            return;
        }
        if (user.getStatus() != null && !"Active".equalsIgnoreCase(user.getStatus())) {
            denied(req, res, "Your account is not allowed to create new orders.");
            return;
        }

        // 2. Kiểm tra vai trò theo đường dẫn
        String role = user.getRole();
        boolean allowed = true;
        if (path.startsWith("/admin") || path.startsWith("/manager")) {
            allowed = role.equals("Administrator") || role.equals("Branch Management");
        } else if (path.startsWith("/pos/print")) {
            // In lại vé: thu ngân, soát vé, quản lý
            allowed = !role.equals("Customer");
        } else if (path.startsWith("/pos/shift")) {
            allowed = role.equals("Ticket Cashier") || role.equals("Ticket Checker");
        } else if (path.startsWith("/pos")) {
            allowed = role.equals("Ticket Cashier");
        } else if (path.startsWith("/checker")) {
            allowed = !role.equals("Customer");
        }
        // /payment/* : mọi người đã đăng nhập (chủ đơn được kiểm tra trong Service)

        if (!allowed) {
            denied(req, res, "You do not have permission to perform this action.");
            return;
        }
        req.setAttribute("me", user);   // JSP dùng ${me.fullName}, ${me.role}
        chain.doFilter(request, response);
    }

    private void denied(HttpServletRequest req, HttpServletResponse res, String message)
            throws ServletException, IOException {
        res.setStatus(403);
        req.setAttribute("errorMsg", message);
        req.getRequestDispatcher("/WEB-INF/views/common/error-nam.jsp").forward(req, res);
    }
}
