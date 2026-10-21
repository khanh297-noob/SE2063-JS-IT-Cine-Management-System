package cinemanagement.controller.movie;

import cinemanagement.controller.BaseServlet;
import cinemanagement.model.movie.Genre;
import cinemanagement.service.movie.GenreService;
import cinemanagement.service.movie.MovieService;
import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Manage Genre.
 * GET  /manage/genres[?id=]                 card chứa các thể loại; có id thì hiện chi tiết + phim thuộc thể loại
 * GET  /manage/genres?action=create|edit&id=
 * POST action=save | delete
 */
@WebServlet("/manage/genres")
public class GenreServlet extends BaseServlet {

    private final GenreService genreService = new GenreService();
    private final MovieService movieService = new MovieService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "list")) {
            case "create" -> showForm(req, resp, new Genre());
            case "edit" -> showForm(req, resp, genreService.get(requiredId(req, "id")));
            default -> {
                req.setAttribute("genres", genreService.list());
                Integer id = intParam(req, "id");
                if (id != null) {
                    req.setAttribute("selected", genreService.get(id));
                    req.setAttribute("genreMovies", movieService.list(null, null, id));
                }
                render(req, resp, "genre/list-hoang");
            }
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        if ("delete".equals(action(req, "save"))) {
            GenreService.DeleteResult r = genreService.delete(requiredId(req, "id"));
            StringBuilder msg = new StringBuilder("Đã xoá thể loại " + r.getGenreName() + ".");
            if (r.getDetached() > 0) msg.append(" Đã gỡ khỏi ").append(r.getDetached()).append(" phim.");
            if (!r.getLeftWithoutGenre().isEmpty())
                msg.append(" Phim không còn thể loại nào và chưa được phát sóng: ")
                   .append(String.join(", ", r.getLeftWithoutGenre())).append(".");
            if (!r.getStoppedShowing().isEmpty())
                msg.append(" Đã chuyển về Sắp chiếu: ").append(String.join(", ", r.getStoppedShowing())).append(".");
            flashSuccess(req, msg.toString());
            redirect(req, resp, "/manage/genres");
            return;
        }
        Genre g = new Genre();
        Integer id = intParam(req, "id");
        g.setGenreId(id == null ? 0 : id);
        g.setName(str(req, "name"));
        g.setDescription(str(req, "description"));
        try {
            int savedId = genreService.save(g);
            flashSuccess(req, (id == null ? "Đã thêm thể loại " : "Đã cập nhật thể loại ") + g.getName() + ".");
            redirect(req, resp, "/manage/genres?id=" + savedId);
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            showForm(req, resp, g);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Genre g)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("genre", g);
        req.setAttribute("existingGenres", genreService.list()); // để form báo trùng tên ngay khi gõ
        render(req, resp, "genre/form-hoang");
    }
}
