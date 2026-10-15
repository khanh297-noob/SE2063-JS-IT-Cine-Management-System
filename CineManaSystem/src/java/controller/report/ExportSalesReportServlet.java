package controller.report;

import dao.ReportDao;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dao.ReportDao.TicketReportItem;
import service.ReportAccessService;
import util.ExportUtil;
import util.PermissionUtil;

@WebServlet(name = "exportSalesReport", urlPatterns = {"/report/export-sales"})
public class ExportSalesReportServlet extends HttpServlet {

    private final ReportDao reportDao = new ReportDao();
    private final ReportAccessService accessService = new ReportAccessService(reportDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        String branchIdParam = request.getParameter("branchId");
        String movieIdParam = request.getParameter("movieId");
        String fromDate = request.getParameter("fromDate");
        String toDate = request.getParameter("toDate");

        Integer branchId = null;
        if (branchIdParam != null && !branchIdParam.trim().isEmpty()) {
            try {
                branchId = Integer.parseInt(branchIdParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        Integer movieId = null;
        if (movieIdParam != null && !movieIdParam.trim().isEmpty()) {
            try {
                movieId = Integer.parseInt(movieIdParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(
                    PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request), branchId);
        } catch (SecurityException ex) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        List<TicketReportItem> reportItems = reportDao.getTicketSalesReport(branchScope, movieId, fromDate, toDate);
        String csvContent = ExportUtil.generateTicketSalesCsv(reportItems);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "BaoCaoDoanhThuVe_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (OutputStream os = response.getOutputStream()) {
            os.write(csvContent.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }
}
