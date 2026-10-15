package filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dao.UserDao;
import model.User;
import util.PermissionUtil;
import util.SessionUtil;

@WebFilter(filterName = "authFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/login",
        "/register",
        "/verify-email",
        "/resend-otp",
        "/logout",
        "/access-denied",
        "/index.html",
        "/index.jsp",
        "/",
        "/forgot-password"
);

    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/css/",
            "/js/",
            "/images/",
            "/fonts/",
            "/static/",
            "/assets/",
            "/webjars/"
    );

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        String servletPath = req.getServletPath();

        // 1. Tài nguyên tĩnh
        if (isPublicResource(servletPath)) {
            chain.doFilter(req, resp);
            return;
        }

        // 2. Đường dẫn công khai (login, register, verify-email...)
        if (isPublicPath(servletPath)) {
            chain.doFilter(req, resp);
            return;
        }

        // 3. Kiểm tra đăng nhập
        if (!PermissionUtil.isLoggedIn(req)) {
            String requestedUrl = req.getRequestURI();
            if (req.getQueryString() != null) {
                requestedUrl += "?" + req.getQueryString();
            }
            req.getSession(true).setAttribute("redirectAfterLogin", requestedUrl);
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // 3b. Kiểm tra lại trạng thái tài khoản ở MỌI request được bảo vệ:
        //     tài khoản bị khóa/vô hiệu hóa/đổi quyền sẽ có hiệu lực ngay, không đợi hết session
        User sessionUser = PermissionUtil.getCurrentUser(req);
        User freshUser = loadUser(sessionUser.getUserID());
        if (freshUser == null || !"Active".equalsIgnoreCase(freshUser.getStatus())) {
            SessionUtil.logout(req);
            req.getSession(true).setAttribute("loginNotice",
                    "Tài khoản của bạn đã bị khóa hoặc không còn hiệu lực. Vui lòng liên hệ hỗ trợ!");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        SessionUtil.login(req, freshUser);

        // 4. Kiểm tra quyền xem Báo cáo doanh thu vé (/report/**)
        if (servletPath.startsWith("/report/")) {
            if (!PermissionUtil.canViewSalesReport(req)) {
                resp.sendRedirect(req.getContextPath() + "/access-denied");
                return;
            }
        }

        // 5. Kiểm tra quyền Admin (/admin/**)
        if (servletPath.startsWith("/admin/") || servletPath.equals("/admin") || servletPath.equals("/test-email")) {
            if (!PermissionUtil.isAdmin(req)) {
                resp.sendRedirect(req.getContextPath() + "/access-denied");
                return;
            }
        }

        // 6. Kiểm tra quyền Nhân viên/Quản lý chi nhánh (/staff/**)
        if (servletPath.startsWith("/staff/") || servletPath.equals("/staff")) {
            if (!PermissionUtil.isInternalUser(req)) {
                resp.sendRedirect(req.getContextPath() + "/access-denied");
                return;
            }
        }

        // 7. Cho phép tiếp tục
        chain.doFilter(req, resp);
    }

    private User loadUser(int userId) {
        UserDao dao = new UserDao();
        try {
            return dao.findById(userId);
        } finally {
            dao.close();
        }
    }

    private boolean isPublicPath(String servletPath) {
        if (servletPath == null || servletPath.isEmpty()) return true;
        for (String path : PUBLIC_PATHS) {
            if (servletPath.equalsIgnoreCase(path)) return true;
        }
        return false;
    }

    private boolean isPublicResource(String servletPath) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (servletPath.startsWith(prefix)) return true;
        }
        if (servletPath.contains(".") && servletPath.indexOf("/", 1) < 0) return true;
        return false;
    }
}
