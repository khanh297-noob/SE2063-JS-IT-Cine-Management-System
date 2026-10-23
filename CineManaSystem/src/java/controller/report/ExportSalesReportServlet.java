package controller.report;

import dao.ReportDao;
import dao.ReportDao.TicketReportItem;
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
import service.ReportAccessService;
import util.ExportUtil;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Xuất danh sách vé đã bán ra file CSV.
 */
@WebServlet(name = "exportSalesReport", urlPatterns = {"/report/export-sales"})
public class ExportSalesReportServlet extends HttpServlet {

    private ReportDao reportDao;
    private ReportAccessService accessService;

    @Override
    public void init() throws ServletException {
        reportDao = new ReportDao();
        accessService = new ReportAccessService(reportDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //kiểm tra quyền
        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);

        //đọc bộ lọc
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));
        Integer movieId = ParamUtil.parseIntOrNull(request.getParameter("movieId"));
        String fromDate = request.getParameter("fromDate");
        String toDate = request.getParameter("toDate");

        //xác định chi nhánh được xem
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, PermissionUtil.isAdmin(request), branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        //lấy danh sách vé rồi chuyển thành CSV
        List<TicketReportItem> items = reportDao.getTicketSalesReport(branchScope, movieId, fromDate, toDate);
        String csv = ExportUtil.generateTicketSalesCsv(items);

        //trả file về trình duyệt
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"BaoCaoDoanhThuVe_" + timestamp + ".csv\"");
        try (OutputStream out = response.getOutputStream()) {
            out.write(csv.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
}
