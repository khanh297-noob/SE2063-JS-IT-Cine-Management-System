package cinemanagement.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import cinemanagement.model.User;
import cinemanagement.util.Web;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Kiem tra dang nhap + phan quyen theo URL (SDS: AuthFilter + RoleFilter).
 *   /manager/*  : Branch Management, Administrator
 *   /pos/*      : Ticket Cashier
 *   /booking/*  : Customer, Ticket Cashier (detail: moi user da dang nhap, quyen xem kiem tra trong service)
 *   /ticket/*   : moi user da dang nhap (kiem tra quyen so huu trong servlet)
 *   /profile/*  : Customer
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(req, resp);
            return;
        }
        User user = Web.currentUser(req);
        if (user == null) {
            if (isAjax(req, path)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String next = path + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
            Web.flash(req, "info", "Vui lòng đăng nhập để tiếp tục.");
            resp.sendRedirect(req.getContextPath() + "/login?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
            return;
        }
        if (!allowed(user, path)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này.");
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
            return;
        }
        chain.doFilter(req, resp);
    }

    private boolean isPublic(String path) {
        return path.isEmpty() || path.equals("/") || path.equals("/home") || path.equals("/login")
                || path.equals("/logout") || path.startsWith("/assets/") || path.equals("/favicon.ico");
    }

    private boolean isAjax(HttpServletRequest req, String path) {
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
                || path.endsWith("/seat-status") || path.endsWith("/check-overlap");
    }

    private boolean allowed(User u, String path) {
        if (path.startsWith("/manager/")) {
            return u.isManager() || u.isAdmin();
        }
        if (path.startsWith("/pos")) {
            return u.isCashier();
        }
        if (path.startsWith("/booking/detail") || path.startsWith("/ticket/")) {
            return true;
        }
        if (path.startsWith("/booking/")) {
            return u.isCustomer() || u.isCashier() || (path.startsWith("/booking/cancel") && u.isAdmin());
        }
        if (path.startsWith("/profile")) {
            return u.isCustomer();
        }
        return true;
    }
}
