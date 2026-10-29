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
 * Kiểm tra mã khuyến mãi có áp dụng được cho đơn hàng không (Thu ngân POS, Quản lý chi nhánh).
 * GET : form nhập mã và giá trị đơn.
 * POST: kiểm tra rồi hiển thị kết quả ngay trên trang (không thay đổi dữ liệu nên không cần redirect).
 *
 * Chi nhánh lấy từ tài khoản ([User].BranchID), người dùng không tự chọn được.
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
        Integer branchId = user.getBranchID();

        ReportDao reportDao = new ReportDao();
        try {
            // Bước 2: dữ liệu cho khung giao diện
            List<Integer> scope = BranchScope.allowed(user, false);
            List<Branch> branches = reportDao.getBranches(scope);
            String pill = new BranchReportService(reportDao).scopePill(scope);
            ManagerShell.apply(request, user, false, pill, branches, branchId, "promo-validate", PAGE);
            request.setAttribute("branchName", branches.isEmpty() ? null : branches.get(0).getName());
            request.setAttribute("noBranch", branchId == null);

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
