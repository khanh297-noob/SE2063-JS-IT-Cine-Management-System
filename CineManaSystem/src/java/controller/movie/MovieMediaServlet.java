package controller.movie;

import model.movie.MovieMedia;
import service.movie.MovieMediaService;
import service.movie.MovieService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Manage Poster/Trailer.
 * GET  /manage/movie/media?movieId=1
 * POST action=add (movieId, mediaType, url, caption, primary) | primary (mediaId) | delete (mediaId)
 */
@WebServlet("/manage/movie/media")
public class MovieMediaServlet extends MovieAdminServlet {

    private final MovieService movieService = new MovieService();
    private final MovieMediaService mediaService = new MovieMediaService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        show(req, resp, requiredId(req, "movieId"), null);
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int movieId = requiredId(req, "movieId");
        switch (action(req, "add")) {
            case "primary" -> {
                mediaService.setPrimary(requiredId(req, "mediaId"));
                flashSuccess(req, "Đã đổi media chính.");
            }
            case "delete" -> {
                mediaService.delete(requiredId(req, "mediaId"));
                flashSuccess(req, "Đã xoá.");
            }
            default -> {
                MovieMedia m = new MovieMedia();
                m.setMovieId(movieId);
                m.setMediaType(str(req, "mediaType"));
                m.setUrl(str(req, "url"));
                m.setCaption(str(req, "caption"));
                m.setPrimary(req.getParameter("primary") != null);
                try {
                    mediaService.add(m);
                    flashSuccess(req, MovieMedia.TYPE_POSTER.equals(m.getMediaType()) ? "Đã thêm poster." : "Đã thêm trailer.");
                } catch (BusinessException e) {
                    req.setAttribute("errors", e.getErrors());
                    show(req, resp, movieId, m);
                    return;
                }
            }
        }
        redirect(req, resp, "/manage/movie/media?movieId=" + movieId);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, int movieId, MovieMedia draft)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("movie", movieService.get(movieId));
        req.setAttribute("mediaList", mediaService.list(movieId));
        req.setAttribute("draft", draft);
        render(req, resp, "movie/media-hoang");
    }
}
