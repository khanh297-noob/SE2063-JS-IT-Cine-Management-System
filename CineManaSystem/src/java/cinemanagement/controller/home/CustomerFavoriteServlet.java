package cinemanagement.controller.home;

import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Ô yêu thích: bật/tắt lượt yêu thích của người xem cho một phim.
 * Lượt yêu thích là một dòng liked = 1 trong MovieReviews; session nhớ id dòng đó để bỏ thích.
 * POST /movies/favorite  movieId, back (trang quay lại)
 */
@WebServlet("/movies/favorite")
public class CustomerFavoriteServlet extends CustomerBaseServlet {

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int movieId = requiredId(req, "movieId");
        Map<Integer, Integer> liked = likedMovies(req);
        Integer likeId = liked.remove(movieId);
        if (likeId != null) {
            customerService.unlike(movieId, likeId);
        } else {
            Object name = req.getSession().getAttribute(REVIEWER_NAME_KEY);
            liked.put(movieId, customerService.like(movieId, name == null ? null : name.toString()));
        }
        redirect(req, resp, safeBack(str(req, "back")));
    }
}
