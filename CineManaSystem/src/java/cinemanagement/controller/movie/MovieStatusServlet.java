package cinemanagement.controller.movie;

import cinemanagement.controller.BaseServlet;
import cinemanagement.util.BusinessException;
import cinemanagement.service.MovieStatusService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Movie Status.
 * GET  /manage/movies/status?movieId=1
 * POST movieId, status
 */
@WebServlet("/manage/movies/status")
public class MovieStatusServlet extends BaseServlet {

    private final MovieStatusService statusService = new MovieStatusService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("info", statusService.getStatus(requiredId(req, "movieId")));
        render(req, resp, "movie/status-hoang");
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int movieId = requiredId(req, "movieId");
        String target = str(req, "status");
        try {
            statusService.changeStatus(movieId, target);
            flashSuccess(req, "Đã chuyển trạng thái sang \"" + target + "\".");
            redirect(req, resp, "/manage/movies/status?movieId=" + movieId);
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("info", statusService.getStatus(movieId));
            render(req, resp, "movie/status-hoang");
        }
    }
}
