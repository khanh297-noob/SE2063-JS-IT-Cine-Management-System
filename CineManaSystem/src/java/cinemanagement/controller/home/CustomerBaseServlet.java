package cinemanagement.controller.home;

import cinemanagement.controller.BaseServlet;
import cinemanagement.service.movie.CustomerMovieService;
import cinemanagement.util.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp cha cho trang Phim của khách (/movies...). Vào tự do, không cần đăng nhập.
 *
 * Lượt yêu thích:
 * - Lưu trong bảng MovieReviews (dòng liked = 1); tổng lượt đọc từ DB nên không mất khi khởi động lại.
 * - HttpSession, khoá "likedMovies": movie_id -> review_id của dòng yêu thích người xem này đã tạo,
 *   để mỗi người chỉ được 1 lượt / phim và bấm lại thì bỏ đúng lượt của mình.
 *
 * Đăng nhập do module Auth (Tuyết) xử lý; ở đây chỉ đọc session và điều hướng:
 * - Session "User" (SessionUtil của Auth): người đang đăng nhập, có getRole() / getFullName().
 * - Session "redirectAfterLogin": Login của Auth chuyển nhân sự nội bộ về địa chỉ này sau khi đăng nhập.
 *   Trang khách đặt sẵn AFTER_LOGIN_URL để CustomerMovieServlet quyết định: Admin -> /manage, còn lại -> trang chủ.
 */
public abstract class CustomerBaseServlet extends BaseServlet {

    static final String USER_KEY = "User";
    static final String REDIRECT_AFTER_LOGIN_KEY = "redirectAfterLogin";
    static final String AFTER_LOGIN_URL = "/movies?afterLogin=1";

    static final String LIKED_KEY = "likedMovies";
    /** Tên người đánh giá gần nhất, điền sẵn vào form đánh giá lần sau. */
    static final String REVIEWER_NAME_KEY = "reviewerName";

    protected final CustomerMovieService customerService = new CustomerMovieService();

    /** movie_id -> review_id của lượt yêu thích người xem hiện tại đã tạo. */
    @SuppressWarnings("unchecked")
    protected Map<Integer, Integer> likedMovies(HttpServletRequest req) {
        HttpSession session = req.getSession();
        synchronized (session) {
            Object v = session.getAttribute(LIKED_KEY);
            if (v instanceof Map) return (Map<Integer, Integer>) v;
            Map<Integer, Integer> map = new ConcurrentHashMap<>();
            session.setAttribute(LIKED_KEY, map);
            return map;
        }
    }

    /** Dữ liệu dùng chung: menu lọc, phim đã thích, tổng lượt yêu thích (từ DB). */
    protected void prepareLayout(HttpServletRequest req) throws SQLException, BusinessException {
        req.setAttribute("menu", customerService.getMenu());
        req.setAttribute("likedMovies", likedMovies(req));
        req.setAttribute("favoriteCounts", customerService.likeCounts());
        req.setAttribute("isAdmin", isAdmin(req));
        if (currentRole(req) == null) {
            req.getSession().setAttribute(REDIRECT_AFTER_LOGIN_KEY, req.getContextPath() + AFTER_LOGIN_URL);
        }
    }

    /**
     * Vai trò của người đang đăng nhập, null nếu chưa đăng nhập.
     * Gọi getRole() qua reflection để module này không phụ thuộc class User của module Auth.
     */
    static String currentRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute(USER_KEY);
        if (user == null) return null;
        try {
            Object role = user.getClass().getMethod("getRole").invoke(user);
            return role == null ? "" : role.toString().trim();
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    /** Cùng quy ước với PermissionUtil.isAdmin của module Auth. */
    static boolean isAdmin(HttpServletRequest req) {
        String role = currentRole(req);
        return "Admin".equalsIgnoreCase(role) || "Administrator".equalsIgnoreCase(role);
    }

    /** Chỉ chấp nhận đường dẫn quay lại trong /movies, tránh chuyển hướng ra ngoài. */
    static String safeBack(String back) {
        if (back == null || !back.startsWith("/movies") || back.contains("//") || back.contains("\\")) return "/movies";
        return back;
    }
}
