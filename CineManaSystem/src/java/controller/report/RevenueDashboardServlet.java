package controller.report;

import com.google.gson.Gson;
import dao.ReportDao;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Branch;
import service.RevenueDashboardService.RevenueDashboard;
import service.BranchReportService;
import service.ReportAccessService;
import service.RevenueDashboardService;
import util.ManagerShell;
import util.PermissionUtil;

@WebServlet(name = "revenueDashboard", urlPatterns = {"/report/revenue-dashboard"})
public class RevenueDashboardServlet extends HttpServlet {

    private final ReportDao reportDao = new ReportDao();
    private final ReportAccessService accessService = new ReportAccessService(reportDao);
    private final RevenueDashboardService dashboardService = new RevenueDashboardService(reportDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // Mặc định: từ ngày đầu đến ngày cuối của tháng hiện tại
        LocalDate today = LocalDate.now();
        LocalDate from = RevenueDashboardService.parseDate(request.getParameter("fromDate"));
        LocalDate to = RevenueDashboardService.parseDate(request.getParameter("toDate"));
        String filterError = null;

        if (from == null) {
            from = today.withDayOfMonth(1);
        }
        if (to == null) {
            to = today.withDayOfMonth(today.lengthOfMonth());
        }
        if (from.isAfter(to)) {
            filterError = "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc. Đã dùng khoảng thời gian mặc định.";
            from = today.withDayOfMonth(1);
            to = today.withDayOfMonth(today.lengthOfMonth());
        } else if (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1 > RevenueDashboardService.MAX_RANGE_DAYS) {
            filterError = "Chỉ xem được tối đa " + RevenueDashboardService.MAX_RANGE_DAYS
                    + " ngày mỗi lần. Đã dùng khoảng thời gian mặc định.";
            from = today.withDayOfMonth(1);
            to = today.withDayOfMonth(today.lengthOfMonth());
        }

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
            branchScope = accessService.resolveBranchScope(
                    PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request), branchId);
        } catch (SecurityException ex) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        List<Branch> branches = reportDao.getBranches(
                accessService.allowedBranchIds(
                        PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request)));

        RevenueDashboard dashboard = dashboardService.build(branchScope, from, to);

        Gson gson = new Gson();
        request.setAttribute("dashboard", dashboard);
        request.setAttribute("labelsJson", gson.toJson(dashboard.getWeekLabels()));
        request.setAttribute("ticketJson", gson.toJson(dashboard.getWeeklyTicketRevenue()));
        request.setAttribute("fnbJson", gson.toJson(dashboard.getWeeklyFnbRevenue()));
        ManagerShell.apply(request, PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request),
                new BranchReportService(reportDao).scopePill(branchScope), branches, branchId, "revenue",
                "/report/revenue-dashboard", accessService.isBranchScopingConfigured());
        request.setAttribute("fromDate", from.toString());
        request.setAttribute("toDate", to.toString());
        request.setAttribute("filterError", filterError);

        // Loai bang phan ra dang hien thi (mac dinh: theo phim)
        String breakdown = request.getParameter("breakdown");
        if (breakdown == null || breakdown.trim().isEmpty()) {
            breakdown = "movie";
        }
        request.setAttribute("selectedBreakdown", breakdown);

        request.getRequestDispatcher("/view/report/revenue-dashboard.jsp").forward(request, response);
    }
}
