package controller.report;

import dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import model.User;
import service.BranchReportService;
import service.BranchReportService.BranchReportView;
import service.ReportAccessService;
import util.DateRange;
import util.ExportUtil;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Xuất báo cáo rạp ra file CSV.
 */
@WebServlet(name = "exportBranchReport", urlPatterns = {"/report/export-branch"})
public class ExportBranchReportServlet extends HttpServlet {

    private ReportAccessService accessService;
    private BranchReportService reportService;

    @Override
    public void init() throws ServletException {
        ReportDao reportDao = new ReportDao();
        accessService = new ReportAccessService(reportDao);
        reportService = new BranchReportService(reportDao);
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

        // Bước 2: đọc bộ lọc (xuất file thì khoảng ngày sai phải báo lỗi, không tự đổi sang mặc định)
        DateRange range = DateRange.fromRequest(request);
        if (range.hasError()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Khoảng thời gian không hợp lệ");
            return;
        }
        String type = BranchReportService.normalizeType(request.getParameter("type"));
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));

        // Bước 3: xác định chi nhánh được xem
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, PermissionUtil.isAdmin(request), branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // Bước 4: dựng báo cáo rồi chuyển thành CSV
        BranchReportView report = reportService.build(type, branchScope, range.getFrom(), range.getTo());
        String csv = ExportUtil.generateBranchReportCsv(report);

        // Bước 5: trả file về trình duyệt
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"BaoCaoRap_" + type + "_" + timestamp + ".csv\"");
        try (OutputStream out = response.getOutputStream()) {
            out.write(csv.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
}
