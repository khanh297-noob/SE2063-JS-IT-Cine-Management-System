package cinemanagement.controller;

import cinemanagement.util.BusinessException;
import cinemanagement.service.MovieBranchService;
import cinemanagement.service.MovieBranchService.BranchSelection;
import cinemanagement.service.MovieBranchService.SaveResult;
import cinemanagement.service.MovieService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configure Available Branches.
 * GET  /manage/movies/branches?movieId=1
 * POST movieId, shown[] (mọi chi nhánh trên form), branch[] (chi nhánh được tích), from_{id}, to_{id}
 */
@WebServlet("/manage/movies/branches")
public class MovieBranchServlet extends BaseServlet {

    private final MovieService movieService = new MovieService();
    private final MovieBranchService movieBranchService = new MovieBranchService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int movieId = requiredId(req, "movieId");
        req.setAttribute("movie", movieService.get(movieId));
        req.setAttribute("rows", movieBranchService.getAvailability(movieId));
        render(req, resp, "movie/branches");
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int movieId = requiredId(req, "movieId");
        List<BranchSelection> selections = readSelections(req);
        try {
            SaveResult r = movieBranchService.save(movieId, selections);
            flashSuccess(req, describe(r));
            redirect(req, resp, "/manage/movies/branches?movieId=" + movieId);
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("movie", movieService.get(movieId));
            req.setAttribute("rows", movieBranchService.previewSubmission(movieId, selections));
            render(req, resp, "movie/branches");
        }
    }

    private List<BranchSelection> readSelections(HttpServletRequest req) {
        Set<String> checked = new HashSet<>();
        String[] checkedRaw = req.getParameterValues("branch");
        if (checkedRaw != null) checked.addAll(Arrays.asList(checkedRaw));

        List<BranchSelection> list = new ArrayList<>();
        String[] shown = req.getParameterValues("shown");
        if (shown == null) return list;
        for (String idRaw : shown) {
            try {
                int id = Integer.parseInt(idRaw);
                list.add(new BranchSelection(id, checked.contains(idRaw),
                        req.getParameter("from_" + id), req.getParameter("to_" + id)));
            } catch (NumberFormatException ignored) {
                // bỏ qua id bị sửa tay
            }
        }
        return list;
    }

    private static String describe(SaveResult r) {
        if (r.isEmpty()) return "Không có thay đổi nào để lưu.";
        List<String> parts = new ArrayList<>();
        if (r.getAdded() > 0) parts.add("thêm " + r.getAdded() + " chi nhánh");
        if (r.getRemoved() > 0) parts.add("gỡ " + r.getRemoved() + " chi nhánh");
        if (r.getUpdated() > 0) parts.add("đổi lịch ở " + r.getUpdated() + " chi nhánh");
        return "Đã lưu: " + String.join(", ", parts) + ".";
    }
}
