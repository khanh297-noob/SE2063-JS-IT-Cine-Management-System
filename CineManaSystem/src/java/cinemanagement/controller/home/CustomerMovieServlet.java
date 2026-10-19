package cinemanagement.controller.home;

import cinemanagement.model.MovieSummaryDTO;
import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Giao diện khách hàng: phim đang chiếu và sắp chiếu.
 * GET /movies                            carousel + phim đang chiếu (6 cột) + cột đề cử tháng + phim sắp chiếu
 * GET /movies?genreId= | year= | lang=   chỉ các lưới phim đã lọc (không carousel, không đề cử)
 */
@WebServlet("/movies")
public class CustomerMovieServlet extends CustomerBaseServlet {

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        Integer genreId = intParam(req, "genreId");
        Integer year = intParam(req, "year");
        String lang = str(req, "lang");
        boolean filtered = genreId != null || year != null || lang != null;

        prepareLayout(req);
        List<MovieSummaryDTO> movies = customerService.listShowing(genreId, year, lang);
        req.setAttribute("movies", movies);
        req.setAttribute("comingSoon", customerService.listComingSoon(genreId, year, lang));
        req.setAttribute("filtered", filtered);

        if (filtered) {
            StringBuilder title = new StringBuilder();
            if (genreId != null) title.append("Thể loại: ").append(customerService.getGenre(genreId).getName());
            if (year != null) title.append(title.length() > 0 ? " · " : "").append("Năm phát hành: ").append(year);
            if (lang != null) title.append(title.length() > 0 ? " · " : "").append("Ngôn ngữ: ").append(lang);
            req.setAttribute("filterTitle", title.toString());
            req.setAttribute("customerNav", genreId != null ? "genre" : year != null ? "year" : "lang");
        } else {
            req.setAttribute("recommended", customerService.recommend(movies, customerService.likeCounts()));
            LocalDate today = LocalDate.now();
            req.setAttribute("recommendMonth", String.format("%02d/%d", today.getMonthValue(), today.getYear()));
            req.setAttribute("customerNav", "home");
        }
        render(req, resp, "customer/home-hoang");
    }
}
