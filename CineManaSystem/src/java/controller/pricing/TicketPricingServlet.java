package controller.pricing;

import dao.PricingDao;
import dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import model.Branch;
import model.Movie;
import model.User;
import service.BranchReportService;
import service.PricingService;
import util.BranchScope;
import util.DateRange;
import util.Flash;
import util.ManagerShell;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Quản lý giá vé (Admin, Quản lý chi nhánh).
 * GET : danh sách suất chiếu sắp tới kèm giá gốc (cố định) và giá hiện tại (sửa được), và bảng phụ thu loại phòng, loại ghế.
 * POST: action = updatePrice (một suất) | bulkPrice (nhiều suất theo bộ lọc)
 *              | roomTypeSurcharge | seatTypeSurcharge (hai loại này chỉ Admin, áp dụng cả chuỗi)
 *              | roomTypeAdjust (Quản lý chi nhánh: cộng/trừ phụ thu loại phòng vào giá hiện tại các suất của chi nhánh mình).
 *
 * Quản lý chỉ thao tác trên chi nhánh của mình ([User].BranchID). Giá vé đã bán (Ticket.FinalPrice)
 * không đổi khi đổi giá, chỉ ảnh hưởng vé bán sau đó.
 */
@WebServlet(name = "ticketPricing", urlPatterns = {"/promotion-price-manage/ticket-pricing"})
public class TicketPricingServlet extends HttpServlet {

    private static final String PAGE = "/promotion-price-manage/ticket-pricing";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Bước 1: kiểm tra quyền
        if (!PermissionUtil.canManagePricing(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);
        boolean isAdmin = PermissionUtil.isAdmin(request);

        // Bước 2: đọc bộ lọc (mặc định 14 ngày tới) và xác định chi nhánh được xem
        LocalDate today = LocalDate.now();
        DateRange range = DateRange.fromRequest(request, today, today.plusDays(13));
        Integer movieId = ParamUtil.parseIntOrNull(request.getParameter("movieId"));
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));
        List<Integer> scope;
        try {
            scope = BranchScope.resolve(user, isAdmin, branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // Bước 3: lấy dữ liệu
        ReportDao reportDao = new ReportDao();
        try (PricingService service = new PricingService()) {
            List<Branch> branches = reportDao.getBranches(BranchScope.allowed(user, isAdmin));
            List<Movie> movies = reportDao.getMovies(scope);
            List<PricingDao.ShowtimePrice> rows = service.listShowtimePrices(scope, movieId, range.getFrom(), range.getTo());
            long editableCount = rows.stream().filter(PricingDao.ShowtimePrice::isEditable).count();

            Flash.moveToRequest(request);
            request.setAttribute("rows", rows);
            request.setAttribute("editableCount", editableCount);
            request.setAttribute("truncated", rows.size() >= PricingDao.MAX_ROWS);
            request.setAttribute("maxRows", PricingDao.MAX_ROWS);
            request.setAttribute("movies", movies);
            request.setAttribute("roomTypes", service.listRoomTypes());
            request.setAttribute("seatTypes", service.listSeatTypes());
            request.setAttribute("isAdminUser", isAdmin);
            request.setAttribute("selectedMovieId", movieId);
            request.setAttribute("fromDate", range.getFrom().toString());
            request.setAttribute("toDate", range.getTo().toString());
            request.setAttribute("filterError", range.getError());

            String pill = new BranchReportService(reportDao).scopePill(scope);
            ManagerShell.apply(request, user, isAdmin, pill, branches, branchId, "ticket-pricing", PAGE);
        } finally {
            reportDao.close();
        }
        request.getRequestDispatcher("/WEB-INF/view/promotion-pricing/ticket-pricing-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Bước 1: kiểm tra quyền
        if (!PermissionUtil.canManagePricing(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);
        boolean isAdmin = PermissionUtil.isAdmin(request);
        String action = request.getParameter("action");

        // Bước 2: thực hiện thao tác, kết quả báo qua Flash rồi redirect về trang danh sách
        try (PricingService service = new PricingService()) {
            Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));
            Integer movieId = ParamUtil.parseIntOrNull(request.getParameter("movieId"));
            String price = request.getParameter("price");

            if ("updatePrice".equals(action)) {
                Integer showtimeId = requireId(request.getParameter("showtimeId"));
                service.updateShowtimePrice(BranchScope.allowed(user, isAdmin), showtimeId, price);
                Flash.success(request, "Đã cập nhật giá hiện tại của suất chiếu #" + showtimeId + ".");

            } else if ("bulkPrice".equals(action)) {
                List<Integer> scope = BranchScope.resolve(user, isAdmin, branchId);
                int updated = service.updateShowtimePricesInBulk(scope, movieId,
                        DateRange.parseDate(request.getParameter("fromDate")),
                        DateRange.parseDate(request.getParameter("toDate")), price);
                Flash.success(request, "Đã cập nhật giá hiện tại của " + updated + " suất chiếu.");

            } else if ("roomTypeSurcharge".equals(action)) {
                service.updateRoomTypeSurcharge(isAdmin, requireId(request.getParameter("id")), price);
                Flash.success(request, "Đã cập nhật phụ thu loại phòng.");

            } else if ("roomTypeAdjust".equals(action)) {
                int updated = service.adjustRoomTypePriceForBranch(BranchScope.allowed(user, isAdmin), isAdmin,
                        requireId(request.getParameter("id")), request.getParameter("delta"));
                Flash.success(request, "Đã điều chỉnh phụ thu loại phòng cho " + updated + " suất chiếu của chi nhánh bạn.");

            } else if ("seatTypeSurcharge".equals(action)) {
                service.updateSeatTypeSurcharge(isAdmin, requireId(request.getParameter("id")), price);
                Flash.success(request, "Đã cập nhật phụ thu loại ghế.");

            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ!");
            }
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        response.sendRedirect(request.getContextPath() + PAGE + queryString(request));
    }

    private Integer requireId(String text) {
        Integer id = ParamUtil.parseIntOrNull(text);
        if (id == null) {
            throw new IllegalArgumentException("Thiếu mã cần cập nhật!");
        }
        return id;
    }

    /** Giữ lại bộ lọc đang chọn khi quay về trang danh sách. */
    private String queryString(HttpServletRequest request) {
        StringBuilder query = new StringBuilder();
        for (String name : new String[]{"branchId", "movieId", "fromDate", "toDate"}) {
            String value = request.getParameter(name);
            if (value != null && !value.trim().isEmpty()) {
                query.append(query.length() == 0 ? "?" : "&").append(name).append("=")
                        .append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }
}
