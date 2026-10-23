package controller.home;

import dao.MovieDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import util.SessionUtil;

/**
 * Trang chủ khách hàng: danh sách phim lấy từ database.
 * GET /home?tab=showing|coming   phim đang chiếu (mặc định) hoặc sắp chiếu
 */
@WebServlet(name = "home", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tab = "coming".equals(request.getParameter("tab")) ? "coming" : "showing";

        try (MovieDao movieDao = new MovieDao()) {
            String status = "coming".equals(tab) ? MovieDao.STATUS_COMING_SOON : MovieDao.STATUS_NOW_SHOWING;
            request.setAttribute("movies", movieDao.findByStatus(status));
        }

        request.setAttribute("tab", tab);
        request.setAttribute("currentUser", SessionUtil.getLoggedInUser(request));
        request.getRequestDispatcher("/WEB-INF/view/home/home-tuyet.jsp").forward(request, response);
    }
}
