package cinemanagement.controller.home;

import cinemanagement.controller.BaseServlet;
import cinemanagement.service.BranchService;
import cinemanagement.service.CustomerMovieService;
import cinemanagement.util.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp cha cho Giao diện khách hàng (/movies..., /branch). Vào tự do, không cần đăng nhập.
 *
 * Lượt yêu thích:
 * - Lưu trong bảng MovieReviews (dòng liked = 1); tổng lượt đọc từ DB nên không mất khi khởi động lại.
 * - HttpSession, khoá "likedMovies": movie_id -> review_id của dòng yêu thích người xem này đã tạo,
 *   để mỗi người chỉ được 1 lượt / phim và bấm lại thì bỏ đúng lượt của mình.
 */
public abstract class CustomerBaseServlet extends BaseServlet {

    static final String LIKED_KEY = "likedMovies";
    /** Tên người đánh giá gần nhất, điền sẵn vào form đánh giá lần sau. */
    static final String REVIEWER_NAME_KEY = "reviewerName";

    protected final CustomerMovieService customerService = new CustomerMovieService();
    private final BranchService branchMenuService = new BranchService();

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

    /** Dữ liệu dùng chung: menu lọc, menu chi nhánh, phim đã thích, tổng lượt yêu thích (từ DB). */
    protected void prepareLayout(HttpServletRequest req) throws SQLException, BusinessException {
        req.setAttribute("menu", customerService.getMenu());
        req.setAttribute("menuBranches", branchMenuService.findActive());
        req.setAttribute("likedMovies", likedMovies(req));
        req.setAttribute("favoriteCounts", customerService.likeCounts());
    }

    /** Chỉ chấp nhận đường dẫn quay lại trong /movies, tránh chuyển hướng ra ngoài. */
    static String safeBack(String back) {
        if (back == null || !back.startsWith("/movies") || back.contains("//") || back.contains("\\")) return "/movies";
        return back;
    }
}
