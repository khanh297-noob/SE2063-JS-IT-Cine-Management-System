package controller.report;

import com.google.gson.Gson;
import dao.ReportDao;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Branch;
import service.BranchReportService.BranchReportView;
import service.BranchReportService;
import service.ReportAccessService;
import service.RevenueDashboardService;
import util.ManagerShell;
import util.PermissionUtil;

/** Báo Cáo Rạp (Branch Report Dashboard): doanh thu, theo chi nhánh, theo phim, giờ cao điểm, lấp đầy ghế. */
@WebServlet(name = "branchReport", urlPatterns = {"/report/branch-report"})
public class BranchReportServlet extends HttpServlet {

    private final ReportDao reportDao = new ReportDao();
    private final ReportAccessService accessService = new ReportAccessService(reportDao);
    private final BranchReportService reportService = new BranchReportService(reportDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

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
        } else if (ChronoUnit.DAYS.between(from, to) + 1 > RevenueDashboardService.MAX_RANGE_DAYS) {
            filterError = "Chỉ xem được tối đa " + RevenueDashboardService.MAX_RANGE_DAYS
                    + " ngày mỗi lần. Đã dùng khoảng thời gian mặc định.";
            from = today.withDayOfMonth(1);
            to = today.withDayOfMonth(today.lengthOfMonth());
        }

        String type = BranchReportService.normalizeType(request.getParameter("type"));

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

        BranchReportView report = reportService.build(type, branchScope, from, to);

        Gson gson = new Gson();
        request.setAttribute("report", report);
        request.setAttribute("labelsJson", gson.toJson(report.getChartLabels()));
        request.setAttribute("datasetsJson", gson.toJson(report.getChartDatasets()));
        request.setAttribute("typeLabels", BranchReportService.typeLabels());
        ManagerShell.apply(request, PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request),
                report.getBranchPill(), branches, branchId, "branch-report", "/report/branch-report",
                accessService.isBranchScopingConfigured());
        request.setAttribute("fromDate", from.toString());
        request.setAttribute("toDate", to.toString());
        request.setAttribute("filterError", filterError);

        request.getRequestDispatcher("/view/report/branch-report.jsp").forward(request, response);
    }
}
