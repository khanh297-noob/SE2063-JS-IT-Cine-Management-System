package controller.home;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import service.movie.CustomerMovieService;
import util.BusinessException;
import util.SessionUtil;

/**
 * Trang chủ khách hàng: danh sách phim lấy qua CustomerMovieService (dùng chung với trang /movies),
 * nên cùng quy tắc hiển thị: "Sắp chiếu" chỉ gồm phim có ngày khởi chiếu sau hôm nay (BR-MS-03).
 * GET /home?tab=showing|coming   phim đang chiếu (mặc định) hoặc sắp chiếu
 */
@WebServlet(name = "home", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    private final CustomerMovieService movieService = new CustomerMovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tab = "coming".equals(request.getParameter("tab")) ? "coming" : "showing";
        try {
            request.setAttribute("movies", "coming".equals(tab)
                    ? movieService.listComingSoon(null, null, null)
                    : movieService.listShowing(null, null, null));
        } catch (SQLException | BusinessException e) {
            throw new ServletException("Không tải được danh sách phim: " + e.getMessage(), e);
        }

        request.setAttribute("tab", tab);
        request.setAttribute("currentUser", SessionUtil.getLoggedInUser(request));
        request.getRequestDispatcher("/WEB-INF/view/home/home-tuyet.jsp").forward(request, response);
    }
}
