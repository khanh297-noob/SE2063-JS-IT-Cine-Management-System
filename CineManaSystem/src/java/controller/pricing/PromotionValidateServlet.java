package controller.pricing;

import dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import model.Branch;
import model.User;
import service.BranchDashboardService;
import service.BranchReportService;
import service.PromotionService;
import service.PromotionService.PromotionResult;
import util.BranchScope;
import util.ManagerShell;
import util.PermissionUtil;

/**
 * Kiểm tra mã khuyến mãi có áp dụng được cho đơn hàng không.
 * Cho phép: Admin, Thu ngân POS, Quản lý chi nhánh.
 * GET : form nhập mã và giá trị đơn, kèm danh sách khuyến mãi của chi nhánh đang chọn
 * (Admin chưa chọn chi nhánh thì hiện khuyến mãi của tất cả chi nhánh).
 * POST: kiểm tra rồi hiển thị kết quả ngay trên trang (không thay đổi dữ liệu nên không cần redirect).
 *
 * Chi nhánh lấy từ tài khoản ([User].BranchID).
 * Riêng Admin không có branchID cố định → cho chọn qua param hoặc mặc định lấy chi nhánh đầu tiên.
 */
@WebServlet(name = "validatePromotion", urlPatterns = {"/promotion-price-manage/validate-promotion"})
public class PromotionValidateServlet extends HttpServlet {

    private static final String PAGE = "/promotion-price-manage/validate-promotion";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        show(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        show(request, response, request);
    }

    /** @param submitted khác null khi người dùng vừa bấm Kiểm tra (chứa code và orderAmount). */
    private void show(HttpServletRequest request, HttpServletResponse response, HttpServletRequest submitted)
            throws ServletException, IOException {
        // Bước 1: kiểm tra quyền
        if (!PermissionUtil.canValidatePromotion(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);
        boolean isAdmin = PermissionUtil.isAdmin(request);

        // Admin không có branchID cố định → cho chọn chi nhánh qua param,
        // hoặc mặc định lấy chi nhánh đầu tiên trong danh sách.
        Integer branchId = user.getBranchID();
        if (isAdmin) {
            String paramBranchId = request.getParameter("branchId");
            if (paramBranchId != null && !paramBranchId.isEmpty()) {
                try { branchId = Integer.parseInt(paramBranchId); } catch (NumberFormatException ignored) {}
            }
        }

        ReportDao reportDao = new ReportDao();
        try {
            // Bước 2: dữ liệu cho khung giao diện
            List<Integer> scope = BranchScope.allowed(user, isAdmin);
            List<Branch> branches = reportDao.getBranches(scope);
            String pill = new BranchReportService(reportDao).scopePill(scope);

            // Nếu chỉ có 1 chi nhánh (Quản lý chi nhánh) → dùng chi nhánh đó
            // Admin chưa chọn (branchId == null) → giữ null, hiển thị dropdown để chọn
            if (branchId == null && branches.size() == 1) {
                branchId = branches.get(0).getBranchID();
            }

            ManagerShell.apply(request, user, isAdmin, pill, branches, branchId, "promo-validate", PAGE);

            // Tìm tên chi nhánh đang chọn để hiển thị trên form
            String branchName = null;
            if (branchId != null) {
                final int bid = branchId;
                branchName = branches.stream()
                        .filter(b -> b.getBranchID() == bid)
                        .map(Branch::getName)
                        .findFirst()
                        .orElse(null);
            }
            request.setAttribute("branchName", branchName);
            // noBranch chỉ true khi nhân viên thường chưa được gán chi nhánh (Admin luôn có thể chọn)
            request.setAttribute("noBranch", branchId == null && !isAdmin);

            // Danh sách khuyến mãi: của chi nhánh đang chọn, hoặc của tất cả chi nhánh khi Admin chưa chọn chi nhánh
            if (branchId != null || isAdmin) {
                try (PromotionService listService = new PromotionService()) {
                    request.setAttribute("promotions",
                            listService.listPromotions(branchId, LocalDate.now(BranchDashboardService.ZONE)));
                }
            }
            request.setAttribute("promoAllBranches", branchId == null);

            // Bước 3: kiểm tra mã khi người dùng bấm nút
            if (submitted != null && branchId != null) {
                String code = request.getParameter("code");
                String orderAmount = request.getParameter("orderAmount");
                request.setAttribute("code", code);
                request.setAttribute("orderAmount", orderAmount);
                try (PromotionService service = new PromotionService()) {
                    PromotionResult result = service.validate(code, branchId, orderAmount,
                            LocalDate.now(BranchDashboardService.ZONE));
                    request.setAttribute("result", result);
                } catch (IllegalArgumentException e) {
                    request.setAttribute("formError", e.getMessage());
                }
            }
        } finally {
            reportDao.close();
        }
        request.getRequestDispatcher("/WEB-INF/view/promotion-pricing/validate-promotion-tuyet.jsp").forward(request, response);
    }
}
