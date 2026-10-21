package filter;

import dao.UserDao;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import model.User;
import util.PermissionUtil;
import util.SessionUtil;

/**
 * Chạy trước MỌI request: kiểm tra đăng nhập, trạng thái tài khoản và quyền
 * truy cập.
 */
@WebFilter(filterName = "authFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    // Các trang ai cũng vào được, không cần đăng nhập
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/login", "/register", "/verify-email", "/resend-otp", "/logout",
            "/access-denied", "/forgot-password", "/index.html", "/index-tuyet.jsp", "/");

    // Các thư mục chứa file tĩnh (css, js, ảnh...)
    private static final List<String> STATIC_PREFIXES = Arrays.asList(
            "/css/", "/js/", "/images/", "/fonts/", "/static/", "/assets/", "/webjars/");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getServletPath();

        // Bước 1: trang công khai và file tĩnh thì cho qua luôn
        if (isStaticResource(path) || isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Bước 2: chưa đăng nhập thì nhớ trang đang vào rồi chuyển sang trang đăng nhập
        if (!PermissionUtil.isLoggedIn(request)) {
            saveRequestedUrl(request);
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Bước 3: nạp lại user từ database ở mỗi request,
        // nên tài khoản bị khóa hoặc đổi vai trò sẽ có hiệu lực ngay
        User sessionUser = PermissionUtil.getCurrentUser(request);
        User freshUser = loadUserFromDatabase(sessionUser.getUserID());
        if (freshUser == null || !"Active".equalsIgnoreCase(freshUser.getStatus())) {
            SessionUtil.logout(request);
            request.getSession(true).setAttribute("loginNotice",
                    "Tài khoản của bạn đã bị khóa hoặc không còn hiệu lực. Vui lòng liên hệ hỗ trợ!");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        SessionUtil.login(request, freshUser);

        // Bước 4: kiểm tra quyền theo đường dẫn
        if (!hasPermission(request, path)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // Bước 5: hợp lệ, cho đi tiếp
        chain.doFilter(request, response);
    }

    /**
     * Kiểm tra quyền truy cập theo từng nhóm đường dẫn.
     */
    private boolean hasPermission(HttpServletRequest request, String path) {

        // Trang Admin: chỉ Admin được truy cập
        if (path.equals("/admin-home.jsp")
                || path.startsWith("/admin/")
                || path.equals("/admin")
                || path.equals("/test-email")) {
            return PermissionUtil.isAdmin(request);
        }

        // Trang Branch Manager: Admin và Quản lý chi nhánh
        if (path.equals("/branch-home.jsp")) {
            return PermissionUtil.isAdmin(request)
                    || PermissionUtil.isBranchManager(request);
        }

        // Báo cáo: Admin và Quản lý chi nhánh
        if (path.startsWith("/report/")) {
            return PermissionUtil.canViewSalesReport(request);
        }

        // Chức năng nhân viên nội bộ
        if (path.startsWith("/staff/") || path.equals("/staff")) {
            return PermissionUtil.isInternalUser(request);
        }

        // Các đường dẫn khác: chỉ cần đăng nhập
        return true;
    }

    /**
     * Lưu đường dẫn đang vào để đăng nhập xong quay lại đúng trang đó.
     */
    private void saveRequestedUrl(HttpServletRequest request) {
        String requestedUrl = request.getRequestURI();
        if (request.getQueryString() != null) {
            requestedUrl += "?" + request.getQueryString();
        }
        request.getSession(true).setAttribute("redirectAfterLogin", requestedUrl);
    }

    /**
     * Lấy user mới nhất từ database, dùng xong đóng kết nối.
     */
    private User loadUserFromDatabase(int userId) {
        UserDao userDao = new UserDao();
        try {
            return userDao.findById(userId);
        } finally {
            userDao.close();
        }
    }

    /**
     * Đường dẫn có nằm trong danh sách công khai không (so sánh chính xác).
     */
    private boolean isPublicPath(String path) {
        if (path == null || path.isEmpty()) {
            return true;
        }
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equalsIgnoreCase(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra file tĩnh: CSS, JS, ảnh, font...
     */
    private boolean isStaticResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // Các thư mục chứa file tĩnh
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        // Chỉ cho phép một số loại file tĩnh ở thư mục gốc
        String lowerPath = path.toLowerCase();

        return lowerPath.matches(
                "^/[^/]+\\.(css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|map)$"
        );
    }
}
