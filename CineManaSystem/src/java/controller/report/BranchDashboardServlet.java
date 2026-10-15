package controller.report;

import dao.ReportDao;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Branch;
import service.BranchDashboardService.BranchDashboard;
import model.User;
import service.BranchDashboardService;
import service.ReportAccessService;
import util.ManagerShell;
import util.PermissionUtil;

/** Dashboard Tổng Quan của chi nhánh: trang chủ cho Branch Manager / Admin, kèm đường dẫn tới các màn hình báo cáo. */
@WebServlet(name = "branchDashboard", urlPatterns = {"/report/branch-dashboard"})
public class BranchDashboardServlet extends HttpServlet {

    private final ReportDao reportDao = new ReportDao();
    private final ReportAccessService accessService = new ReportAccessService(reportDao);
    private final BranchDashboardService dashboardService = new BranchDashboardService(reportDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);

        Integer branchId = null;
        String branchParam = request.getParameter("branchId");
        if (branchParam != null && !branchParam.trim().isEmpty()) {
            try {
                branchId = Integer.parseInt(branchParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, PermissionUtil.isAdmin(request), branchId);
        } catch (SecurityException ex) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        List<Branch> branches = reportDao.getBranches(
                accessService.allowedBranchIds(user, PermissionUtil.isAdmin(request)));

        BranchDashboard dashboard = dashboardService.build(branchScope, LocalDateTime.now(BranchDashboardService.ZONE));

        request.setAttribute("dashboard", dashboard);
        ManagerShell.apply(request, user, PermissionUtil.isAdmin(request), dashboard.getBranchPill(), branches,
                branchId, "dashboard", "/report/branch-dashboard", accessService.isBranchScopingConfigured());

        request.getRequestDispatcher("/view/report/branch-dashboard.jsp").forward(request, response);
    }
}
