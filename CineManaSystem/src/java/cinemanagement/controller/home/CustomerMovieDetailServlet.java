package cinemanagement.controller.home;

import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Chi tiết phim đang chiếu: xem thông tin, viết đánh giá, chấm 1-5 sao.
 * GET  /movies/detail?id=
 * POST /movies/detail  id, name, rating, content
 */
@WebServlet("/movies/detail")
public class CustomerMovieDetailServlet extends CustomerBaseServlet {

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        show(req, resp, requiredId(req, "id"));
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int id = requiredId(req, "id");
        String name = str(req, "name");
        Integer rating = intParam(req, "rating");
        String content = text(req, "content");
        try {
            customerService.addReview(id, name, rating, content);
            req.getSession().setAttribute(REVIEWER_NAME_KEY, name.replaceAll("\\s+", " "));
            flashSuccess(req, "Cảm ơn bạn đã đánh giá phim.");
            redirect(req, resp, "/movies/detail?id=" + id + "#reviews");
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("draftName", name);
            req.setAttribute("draftRating", rating);
            req.setAttribute("draftContent", content);
            show(req, resp, id);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, int id)
            throws ServletException, IOException, SQLException, BusinessException {
        prepareLayout(req);
        req.setAttribute("detail", customerService.getDetail(id));
        if (req.getAttribute("draftName") == null)
            req.setAttribute("draftName", req.getSession().getAttribute(REVIEWER_NAME_KEY));
        render(req, resp, "customer/detail-hoang");
    }
}
