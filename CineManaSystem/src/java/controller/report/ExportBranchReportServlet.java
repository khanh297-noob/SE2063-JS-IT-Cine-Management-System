package controller.report;

import dao.ReportDao;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BranchReportService.BranchReportView;
import service.BranchReportService;
import service.ReportAccessService;
import service.RevenueDashboardService;
import util.ExportUtil;
import util.PermissionUtil;

@WebServlet(name = "exportBranchReport", urlPatterns = {"/report/export-branch"})
public class ExportBranchReportServlet extends HttpServlet {

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
        if (from == null) from = today.withDayOfMonth(1);
        if (to == null) to = today.withDayOfMonth(today.lengthOfMonth());
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > RevenueDashboardService.MAX_RANGE_DAYS) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Khoảng thời gian không hợp lệ");
            return;
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

        BranchReportView report = reportService.build(type, branchScope, from, to);
        String csv = ExportUtil.generateBranchReportCsv(report);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"BaoCaoRap_" + type + "_" + timestamp + ".csv\"");

        try (OutputStream os = response.getOutputStream()) {
            os.write(csv.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }
}
