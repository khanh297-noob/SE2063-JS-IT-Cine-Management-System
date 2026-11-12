package controller.home;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import dao.PricingDao.RoomTypePriceBoard;
import service.PricingService;
import service.movie.CustomerMovieService;
import util.BusinessException;
import util.SessionUtil;

/**
 * Trang chủ khách hàng: danh sách phim lấy qua CustomerMovieService (dùng chung với trang /movies),
 * nên cùng quy tắc hiển thị: "Sắp chiếu" chỉ gồm phim có ngày khởi chiếu sau hôm nay (BR-MS-03).
 * GET /home?tab=showing|coming   phim đang chiếu (mặc định) hoặc sắp chiếu
 *
 * Mục "Giá vé" (#pricing) lấy từ PricingService.listPublicPriceBoards():
 *   priceBoards      bảng giá vé của từng loại phòng tại từng chi nhánh (gộp từ Showtime.BasePrice các suất sắp chiếu)
 *   pricingBranches  các chi nhánh có bảng giá (branchId -> tên) dùng cho bộ lọc chi nhánh
 * Lỗi khi tải bảng giá không làm hỏng trang chủ, mục giá vé chỉ hiển thị thông báo.
 */
@WebServlet(name = "home", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    private final CustomerMovieService movieService = new CustomerMovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tab = "coming".equals(request.getParameter("tab")) ? "coming" : "showing";
        try {
            request.setAttribute("movies", "coming".equals(tab)
                    ? movieService.listComingSoon(null, null, null)
                    : movieService.listShowing(null, null, null));
            request.setAttribute("promotions", new service.promotion.PromotionService().publicList());
        } catch (SQLException | BusinessException e) {
            throw new ServletException("Không tải được danh sách phim: " + e.getMessage(), e);
        }

        loadTicketPrices(request);

        request.setAttribute("tab", tab);
        request.setAttribute("currentUser", SessionUtil.getLoggedInUser(request));
        request.getRequestDispatcher("/WEB-INF/view/home/home-tuyet.jsp").forward(request, response);
    }

    private void loadTicketPrices(HttpServletRequest request) {
        Map<Integer, String> branches = new LinkedHashMap<>();
        try (PricingService pricingService = new PricingService()) {
            java.util.List<RoomTypePriceBoard> rows = pricingService.listPublicPriceBoards();
            for (RoomTypePriceBoard row : rows) {
                branches.putIfAbsent(row.getBranchId(), row.getBranchName());
            }
            request.setAttribute("priceBoards", rows);
        } catch (RuntimeException e) {
            request.setAttribute("pricingError", true);
        }
        request.setAttribute("pricingBranches", branches);
    }
}
