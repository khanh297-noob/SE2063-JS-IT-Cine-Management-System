package controller.report;

import dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import model.Branch;
import model.User;
import service.BranchDashboardService;
import service.BranchDashboardService.BranchDashboard;
import service.ReportAccessService;
import util.ManagerShell;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Dashboard tổng quan của chi nhánh: trang chủ của Quản lý chi nhánh / Admin.
 */
@WebServlet(name = "branchDashboard", urlPatterns = {"/report/branch-dashboard"})
public class BranchDashboardServlet extends HttpServlet {

    private ReportDao reportDao;
    private ReportAccessService accessService;
    private BranchDashboardService dashboardService;

    @Override
    public void init() throws ServletException {
        reportDao = new ReportDao();
        accessService = new ReportAccessService(reportDao);
        dashboardService = new BranchDashboardService(reportDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Bước 1: kiểm tra quyền
        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);
        boolean isAdmin = PermissionUtil.isAdmin(request);

        // Bước 2: xác định chi nhánh được xem
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, isAdmin, branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // Bước 3: lấy dữ liệu
        List<Branch> branches = reportDao.getBranches(accessService.allowedBranchIds(user, isAdmin));
        BranchDashboard dashboard = dashboardService.build(branchScope, LocalDateTime.now(BranchDashboardService.ZONE));

        // Bước 4: gửi sang JSP
        request.setAttribute("dashboard", dashboard);
        ManagerShell.apply(request, user, isAdmin, dashboard.getBranchPill(), branches, branchId,
                "dashboard", "/report/branch-dashboard");
        request.getRequestDispatcher("/WEB-INF/view/report/branch-dashboard-tuyet.jsp").forward(request, response);
    }
}
