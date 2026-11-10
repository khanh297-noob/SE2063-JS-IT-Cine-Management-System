package controller.home;

import model.movie.MovieSummaryDTO;
import service.movie.CustomerMovieService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Trang "Phim" của giao diện khách hàng: phim đang chiếu và sắp chiếu (trang chủ "/" là /home).
 * GET /movies                            carousel + phim đang chiếu (6 cột) + cột đề cử tháng + phim sắp chiếu
 * GET /movies?genreId= | year= | lang=   chỉ các lưới phim đã lọc (không carousel, không đề cử)
 * GET /movies?q=                         tìm theo tên phim / đạo diễn / diễn viên (BR-CU-07), kết hợp được với bộ lọc trên
 * GET /movies?afterLogin=1               Login (module Auth) chuyển về đây -> /manage (chia theo vai trò)
 */
@WebServlet("/movies")
public class CustomerMovieServlet extends CustomerBaseServlet {

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        if (req.getParameter("afterLogin") != null) {
            redirect(req, resp, "/manage");
            return;
        }

        Integer genreId = intParam(req, "genreId");
        Integer year = intParam(req, "year");
        String lang = str(req, "lang");
        String q = CustomerMovieService.normalizeKeyword(req.getParameter("q"));
        boolean filtered = genreId != null || year != null || lang != null || q != null;

        prepareLayout(req);
        List<MovieSummaryDTO> movies = customerService.listShowing(genreId, year, lang, q);
        req.setAttribute("movies", movies);
        req.setAttribute("comingSoon", customerService.listComingSoon(genreId, year, lang, q));
        req.setAttribute("filtered", filtered);
        req.setAttribute("q", q);
        req.setAttribute("genreId", genreId);
        req.setAttribute("year", year);
        req.setAttribute("lang", lang);

        if (filtered) {
            StringBuilder title = new StringBuilder();
            if (q != null) title.append("Kết quả tìm kiếm: “").append(q).append('”');
            if (genreId != null) title.append(title.length() > 0 ? " · " : "").append("Thể loại: ").append(customerService.getGenre(genreId).getName());
            if (year != null) title.append(title.length() > 0 ? " · " : "").append("Năm phát hành: ").append(year);
            if (lang != null) title.append(title.length() > 0 ? " · " : "").append("Ngôn ngữ: ").append(lang);
            req.setAttribute("filterTitle", title.toString());
            req.setAttribute("customerNav", genreId != null ? "genre" : year != null ? "year" : lang != null ? "lang" : "search");
        } else {
            req.setAttribute("recommended", customerService.recommend(movies, customerService.likeCounts()));
            LocalDate today = LocalDate.now();
            req.setAttribute("recommendMonth", String.format("%02d/%d", today.getMonthValue(), today.getYear()));
            req.setAttribute("customerNav", "home");
        }
        render(req, resp, "customer/home-hoang");
    }
}
