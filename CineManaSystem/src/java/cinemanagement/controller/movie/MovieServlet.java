package cinemanagement.controller.movie;

import cinemanagement.controller.BaseServlet;
import cinemanagement.model.movie.Movie;
import cinemanagement.service.movie.GenreService;
import cinemanagement.service.movie.MovieService;
import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manage Movie.
 * GET  /manage/movie[?q=&status=&genreId=]
 * GET  /manage/movie?action=create | edit&id= | view&id=
 * POST action=save | delete
 */
@WebServlet("/manage/movie")
public class MovieServlet extends BaseServlet {

    private final MovieService movieService = new MovieService();
    private final GenreService genreService = new GenreService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "list")) {
            case "create" -> showForm(req, resp, new Movie(), new LinkedHashSet<>());
            case "view" -> {
                req.setAttribute("info", movieService.getSummary(requiredId(req, "id")));
                render(req, resp, "movie/detail-hoang");
            }
            case "edit" -> {
                int id = requiredId(req, "id");
                showForm(req, resp, movieService.get(id), movieService.getGenreIds(id));
            }
            default -> {
                String q = str(req, "q");
                String status = str(req, "status");
                Integer genreId = intParam(req, "genreId");
                req.setAttribute("movies", movieService.list(q, status, genreId));
                req.setAttribute("genres", genreService.list());
                req.setAttribute("q", q);
                req.setAttribute("status", status);
                req.setAttribute("genreId", genreId);
                render(req, resp, "movie/list-hoang");
            }
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        if ("delete".equals(action(req, "save"))) {
            movieService.delete(requiredId(req, "id"));
            flashSuccess(req, "Đã xoá phim.");
            redirect(req, resp, "/manage/movie");
            return;
        }

        Movie m = new Movie();
        Integer id = intParam(req, "id");
        m.setMovieId(id == null ? 0 : id);
        m.setTitle(str(req, "title"));
        m.setDescription(text(req, "description"));
        m.setDirector(str(req, "director"));
        m.setCastMembers(str(req, "castMembers"));
        m.setLanguage(str(req, "language"));
        m.setDuration(intParam(req, "duration"));
        m.setReleaseDate(dateParam(req, "releaseDate"));
        m.setAgeRating(str(req, "ageRating"));

        Set<Integer> genreIds = new LinkedHashSet<>();
        String[] raw = req.getParameterValues("genreIds");
        if (raw != null) {
            for (String s : raw) {
                try {
                    genreIds.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                    // bỏ qua giá trị bị sửa tay
                }
            }
        }

        try {
            int savedId = movieService.save(m, genreIds);
            if (id == null) {
                flashSuccess(req, "Đã thêm \"" + m.getTitle() + "\". Tiếp theo, thêm poster và trailer.");
                redirect(req, resp, "/manage/movie/media?movieId=" + savedId);
            } else {
                flashSuccess(req, "Đã cập nhật \"" + m.getTitle() + "\".");
                redirect(req, resp, "/manage/movie");
            }
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            if (id != null) m.setStatus(movieService.get(id).getStatus());
            showForm(req, resp, m, genreIds);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Movie m, Set<Integer> selectedGenres)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("movie", m);
        req.setAttribute("genres", genreService.list());
        req.setAttribute("selectedGenres", selectedGenres);
        req.setAttribute("ageRatings", Movie.AGE_RATINGS);
        render(req, resp, "movie/form-hoang");
    }
}
