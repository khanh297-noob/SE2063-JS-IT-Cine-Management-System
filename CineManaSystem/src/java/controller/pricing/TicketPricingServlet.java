package controller.pricing;

import dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Branch;
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
 * Quản lý giá vé (Admin, Quản lý chi nhánh). GET : phần cấu hình giá hệ thống
 * (giá cơ bản theo loại ngày, phụ thu loại ghế, loại phòng) và các ô điều chỉnh
 * của quản lý chi nhánh (cộng/trừ phụ thu loại phòng, phụ thu loại ghế riêng
 * của chi nhánh). Trang không còn danh sách suất chiếu; hai action updatePrice
 * / bulkPrice bên dưới giữ lại nhưng chưa có giao diện.
 *
 * Công thức: Showtime.BasePrice = giá cơ bản theo ngày + phụ thu loại phòng.
 *
 * POST: action = saveConfig (chỉ Admin, áp dụng cả chuỗi) | updatePrice (một
 * suất) | bulkPrice (nhiều suất theo bộ lọc) | roomTypeSurcharge |
 * seatTypeSurcharge (đổi riêng từng loại, chỉ Admin; trang hiện dùng
 * saveConfig) | seatTypeBranch | seatTypeBranchReset (Quản lý chi nhánh: đặt /
 * bỏ phụ thu loại ghế riêng của chi nhánh mình) | roomTypeAdjust (Quản lý chi
 * nhánh: cộng/trừ phụ thu loại phòng vào giá các suất của chi nhánh mình).
 *
 * Quản lý chỉ thao tác trên chi nhánh của mình ([User].BranchID). Giá vé đã bán
 * (Ticket.FinalPrice) không đổi khi đổi giá, chỉ ảnh hưởng vé bán sau đó.
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

        // Bước 2: xác định chi nhánh được xem (Quản lý chỉ chi nhánh của mình)
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

            Flash.moveToRequest(request);
            // Lấy phụ thu loại ghế / loại phòng: nếu chọn chi nhánh cụ thể thì lấy của chi nhánh đó, ngược lại lấy phụ thu chung
            Integer targetBranchId = (!isAdmin && user.getBranchID() != null) ? user.getBranchID() : branchId;
            request.setAttribute("targetBranchId", targetBranchId);
            request.setAttribute("roomTypes", targetBranchId != null
                    ? service.listRoomTypesForBranch(targetBranchId) : service.listRoomTypes());
            request.setAttribute("seatTypes", targetBranchId != null
                    ? service.listSeatTypesForBranch(targetBranchId) : service.listSeatTypes());
            // Cấu hình giá hệ thống: nếu xem chi nhánh thì lấy giá ngày của chi nhánh, không thì lấy giá chung
            request.setAttribute("dayPrices", targetBranchId != null
                    ? service.getDayBasePricesForBranch(targetBranchId) : service.getDayBasePrices());
            boolean canEditDayPrices = isAdmin || (!isAdmin && user.getBranchID() != null);
            request.setAttribute("canEditDayPrices", canEditDayPrices);
            request.setAttribute("isAdminUser", isAdmin);

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
            if ("saveConfig".equals(action)) {
                String section = request.getParameter("section");
                if ("day".equals(section) && (!isAdmin || branchId != null)) {
                    int shifted = service.saveDayBasePricesForBranch(user, isAdmin, branchId,
                            request.getParameter("weekdayPrice"), request.getParameter("weekendPrice"));
                    Flash.success(request, "Đã cập nhật giá cơ bản theo loại ngày cho chi nhánh. "
                            + (shifted > 0 ? ("Giá " + shifted + " suất chiếu sắp tới đã được cập nhật theo.") : ""));
                } else if (isAdmin && branchId != null && ("seat".equals(section) || "room".equals(section))) {
                    // Admin đang chọn một chi nhánh: chỉ đổi phụ thu của chi nhánh đó, không đụng phụ thu chung
                    boolean room = "room".equals(section);
                    int shifted = service.saveBranchConfig(isAdmin, branchId, user.getUserID(), request.getRemoteAddr(),
                            room ? idMap(request, "room_") : Collections.<Integer, String>emptyMap(),
                            room ? Collections.<Integer, String>emptyMap() : idMap(request, "seat_"));
                    Flash.success(request, room
                            ? "Đã lưu phụ thu định dạng phòng cho chi nhánh này. "
                            + (shifted > 0 ? ("Giá " + shifted + " suất chiếu sắp tới của chi nhánh đã được cập nhật theo.") : "")
                            : "Đã lưu phụ thu loại ghế cho chi nhánh này. Chi nhánh khác không bị ảnh hưởng.");
                } else {
                    service.saveConfig(isAdmin,
                            request.getParameter("weekdayPrice"), request.getParameter("weekendPrice"),
                            idMap(request, "room_"), idMap(request, "seat_"));
                    Flash.success(request, configSavedMessage(section));
                }

            } else if ("roomTypeSurcharge".equals(action)) {
                service.updateRoomTypeSurcharge(isAdmin, requireId(request.getParameter("id")), price);
                Flash.success(request, "Đã cập nhật phụ thu loại phòng. Giá cơ bản các suất sắp chiếu giữ nguyên, giá bán đổi theo phụ thu mới.");

            } else if ("roomTypeAdjust".equals(action)) {
                int updated = service.adjustRoomTypePriceForBranch(BranchScope.allowed(user, isAdmin), isAdmin,
                        requireId(request.getParameter("id")), request.getParameter("delta"));
                Flash.success(request, "Đã điều chỉnh phụ thu loại phòng cho " + updated + " suất chiếu của chi nhánh bạn.");

            } else if ("roomTypeBranch".equals(action)) {
                int updated = service.saveRoomTypeSurchargeForBranch(isAdmin, user.getBranchID(),
                        requireId(request.getParameter("id")), price);
                String msg = "Đã lưu phụ thu định dạng phòng cho chi nhánh của bạn.";
                if (updated > 0) {
                    msg += " Đã cập nhật giá " + updated + " suất chiếu sắp tới.";
                }
                Flash.success(request, msg);

            } else if ("roomTypeBranchReset".equals(action)) {
                int updated = service.resetRoomTypeSurchargeForBranch(isAdmin, user.getBranchID(),
                        requireId(request.getParameter("id")));
                String msg = "Chi nhánh của bạn đã quay về dùng phụ thu định dạng phòng chung.";
                if (updated > 0) {
                    msg += " Đã cập nhật giá " + updated + " suất chiếu sắp tới.";
                }
                Flash.success(request, msg);

            } else if ("seatTypeSurcharge".equals(action)) {
                service.updateSeatTypeSurcharge(isAdmin, requireId(request.getParameter("id")), price);
                Flash.success(request, "Đã cập nhật phụ thu loại ghế.");

            } else if ("seatTypeBranch".equals(action)) {
                service.saveSeatTypeSurchargeForBranch(isAdmin, user.getBranchID(), user.getUserID(),
                        request.getRemoteAddr(), requireId(request.getParameter("id")), price);
                Flash.success(request, "Đã cập nhật phụ thu loại ghế cho chi nhánh của bạn.");

            } else if ("seatTypeBranchReset".equals(action)) {
                service.resetSeatTypeSurchargeForBranch(isAdmin, user.getBranchID(), user.getUserID(),
                        request.getRemoteAddr(), requireId(request.getParameter("id")));
                Flash.success(request, "Chi nhánh của bạn đã quay về dùng phụ thu loại ghế chung.");

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

    /**
     * Thông báo sau khi lưu một mục của phần cấu hình giá (mục nào thì nói mục
     * đó).
     */
    private String configSavedMessage(String section) {
        String effect = " Giá các suất chiếu sắp tới bị ảnh hưởng đã được cập nhật theo.";
        if ("day".equals(section)) {
            return "Đã lưu giá cơ bản theo loại ngày." + effect;
        } else if ("seat".equals(section)) {
            return "Đã lưu phụ thu loại ghế. Giá vé áp dụng cho khách đặt vé từ bây giờ.";
        } else if ("room".equals(section)) {
            return "Đã lưu phụ thu định dạng phòng." + effect;
        }
        return "Đã lưu cấu hình giá." + effect;
    }

    /**
     * Gom các ô nhập có tên dạng "prefix + id" (ví dụ room_3, seat_2) thành bản
     * đồ id -> giá trị. Tên ô không đúng dạng thì bỏ qua.
     */
    private Map<Integer, String> idMap(HttpServletRequest request, String prefix) {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            String name = e.getKey();
            if (name.startsWith(prefix)) {
                Integer id = ParamUtil.parseIntOrNull(name.substring(prefix.length()));
                if (id != null && e.getValue().length > 0) {
                    result.put(id, e.getValue()[0]);
                }
            }
        }
        return result;
    }

    private Integer requireId(String text) {
        Integer id = ParamUtil.parseIntOrNull(text);
        if (id == null) {
            throw new IllegalArgumentException("Thiếu mã cần cập nhật!");
        }
        return id;
    }

    /**
     * Giữ lại bộ lọc đang chọn khi quay về trang danh sách.
     */
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
