package controller.pricing;

import dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import model.Branch;
import model.User;
import service.BranchReportService;
import service.PricingService;
import util.BranchScope;
import util.Flash;
import util.ManagerShell;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Quản lý giá F&B (Admin, Quản lý chi nhánh).
 * GET : thực đơn của MỘT chi nhánh: giá gốc, giá riêng của chi nhánh, giá đang áp dụng, trạng thái.
 * POST: action = saveItem (giá riêng + trạng thái của món tại chi nhánh)
 *              | basePrice (giá gốc của món, chỉ Admin).
 *
 * Quản lý chỉ xem và sửa chi nhánh của mình. Admin chọn chi nhánh ở ô chọn trên navbar.
 */
@WebServlet(name = "fnbPricing", urlPatterns = {"/promotion-price-manage/fnb-pricing"})
public class FnbPricingServlet extends HttpServlet {

    private static final String PAGE = "/promotion-price-manage/fnb-pricing";

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

        ReportDao reportDao = new ReportDao();
        try (PricingService service = new PricingService()) {
            // Bước 2: xác định MỘT chi nhánh. Quản lý chọn chi nhánh ngoài quyền thì bị chặn
            Integer requested = ParamUtil.parseIntOrNull(request.getParameter("branchId"));
            BranchScope.resolve(user, isAdmin, requested);
            List<Branch> branches = reportDao.getBranches(BranchScope.allowed(user, isAdmin));
            Branch branch = pickBranch(branches, requested);

            // Bước 3: lấy thực đơn
            Flash.moveToRequest(request);
            request.setAttribute("branch", branch);
            request.setAttribute("menu", branch == null ? null : service.listBranchMenu(branch.getBranchID()));
            request.setAttribute("statuses", PricingService.MENU_STATUSES);
            request.setAttribute("isAdminUser", isAdmin);

            Integer shownBranchId = branch == null ? null : branch.getBranchID();
            String pill = new BranchReportService(reportDao).scopePill(
                    shownBranchId == null ? BranchScope.allowed(user, isAdmin) : java.util.Arrays.asList(shownBranchId));
            ManagerShell.apply(request, user, isAdmin, pill, branches, shownBranchId, "fnb-pricing", PAGE);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        } finally {
            reportDao.close();
        }
        request.getRequestDispatcher("/WEB-INF/view/promotion-pricing/fnb-pricing-tuyet.jsp").forward(request, response);
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
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));

        // Bước 2: thực hiện thao tác, báo kết quả qua Flash rồi redirect về trang thực đơn
        try (PricingService service = new PricingService()) {
            Integer itemId = ParamUtil.parseIntOrNull(request.getParameter("itemId"));
            if (itemId == null) {
                throw new IllegalArgumentException("Thiếu mã món cần cập nhật!");
            }

            if ("saveItem".equals(action)) {
                if (branchId == null) {
                    throw new IllegalArgumentException("Thiếu chi nhánh!");
                }
                BranchScope.resolve(user, isAdmin, branchId); // ngoài quyền thì ném SecurityException
                service.saveBranchItem(branchId, itemId, request.getParameter("price"), request.getParameter("status"));
                Flash.success(request, "Đã lưu giá và trạng thái của món tại chi nhánh.");

            } else if ("basePrice".equals(action)) {
                service.updateItemBasePrice(isAdmin, itemId, request.getParameter("price"));
                Flash.success(request, "Đã cập nhật giá gốc của món (áp dụng cho các chi nhánh chưa đặt giá riêng).");

            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ!");
            }
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        String query = branchId == null ? "" : "?branchId=" + branchId;
        response.sendRedirect(request.getContextPath() + PAGE + query);
    }

    /** Chi nhánh đang xem: chi nhánh được chọn nếu nằm trong danh sách.
     *  Admin chưa chọn (requested == null) → trả null (hiển thị tất cả).
     *  Quản lý chi nhánh không có quyền chọn → luôn lấy chi nhánh duy nhất của họ. */
    private Branch pickBranch(List<Branch> branches, Integer requested) {
        if (requested != null) {
            for (Branch b : branches) {
                if (b.getBranchID() == requested) {
                    return b;
                }
            }
        }
        // Nếu danh sách chỉ có 1 chi nhánh (Quản lý chi nhánh) → luôn chọn chi nhánh đó
        if (branches.size() == 1) {
            return branches.get(0);
        }
        // Admin chưa chọn (requested == null) → null = tất cả chi nhánh
        return null;
    }
}
