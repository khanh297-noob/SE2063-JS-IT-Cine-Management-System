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
import util.BranchScope;
import util.ManagerShell;
import util.PermissionUtil;

/**
 * Trang tổng hợp của feature Promotion & Pricing: liệt kê các chức năng chi tiết người dùng được vào
 * (quản lý giá vé, quản lý giá F&B, kiểm tra mã khuyến mãi). Mỗi chức năng là một trang riêng.
 *
 * Ô "Promotion & Pricing" trên admin-home.jsp trỏ tới /manage/promotion-pricing,
 * còn trên branch-home.jsp trỏ tới /promotion-price-manage, nên trang này nhận cả hai địa chỉ.
 */
@WebServlet(name = "promotionPricingHub", urlPatterns = {"/promotion-price-manage", "/manage/promotion-pricing"})
public class PromotionPricingHubServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Bước 1: kiểm tra quyền (Admin, Quản lý chi nhánh, Thu ngân)
        if (!PermissionUtil.canOpenPromotionPricing(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);
        boolean isAdmin = PermissionUtil.isAdmin(request);

        // Bước 2: dữ liệu cho khung giao diện (navbar, sidebar)
        ReportDao reportDao = new ReportDao();
        try {
            List<Integer> scope = BranchScope.allowed(user, isAdmin);
            List<Branch> branches = reportDao.getBranches(scope);
            String pill = new BranchReportService(reportDao).scopePill(scope);
            ManagerShell.apply(request, user, isAdmin, pill, branches, null, "promo-hub", "/promotion-price-manage");
        } finally {
            reportDao.close();
        }

        // Bước 3: link quay về trang đầu của từng vai trò
        String homePath = isAdmin ? "/admin-home.jsp"
                : PermissionUtil.isBranchManager(request) ? "/branch-home.jsp" : "/index-tuyet.jsp";
        request.setAttribute("manageHomeUrl", request.getContextPath() + homePath);

        request.getRequestDispatcher("/WEB-INF/view/promotion-pricing/hub-tuyet.jsp").forward(request, response);
    }
}
