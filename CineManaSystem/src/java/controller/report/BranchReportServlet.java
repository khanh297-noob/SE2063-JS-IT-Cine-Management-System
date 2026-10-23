package controller.report;

import com.google.gson.Gson;
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
import service.BranchReportService.BranchReportView;
import service.ReportAccessService;
import util.DateRange;
import util.ManagerShell;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Báo cáo rạp: doanh thu, theo chi nhánh, theo phim, giờ cao điểm, lấp đầy ghế.
 * Loại báo cáo chọn bằng tham số type.
 */
@WebServlet(name = "branchReport", urlPatterns = {"/report/branch-report"})
public class BranchReportServlet extends HttpServlet {

    private ReportDao reportDao;
    private ReportAccessService accessService;
    private BranchReportService reportService;

    @Override
    public void init() throws ServletException {
        reportDao = new ReportDao();
        accessService = new ReportAccessService(reportDao);
        reportService = new BranchReportService(reportDao);
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
        boolean isAdmin = PermissionUtil.isAdmin(request);

        //đọc bộ lọc (khoảng ngày, loại báo cáo, chi nhánh)
        DateRange range = DateRange.fromRequest(request);
        String type = BranchReportService.normalizeType(request.getParameter("type"));
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));

        //xác định chi nhánh được xem
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, isAdmin, branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        //dựng báo cáo
        List<Branch> branches = reportDao.getBranches(accessService.allowedBranchIds(user, isAdmin));
        BranchReportView report = reportService.build(type, branchScope, range.getFrom(), range.getTo());

        //gửi sang JSP (dữ liệu biểu đồ chuyển sang JSON cho Chart.js)
        Gson gson = new Gson();
        request.setAttribute("report", report);
        request.setAttribute("labelsJson", gson.toJson(report.getChartLabels()));
        request.setAttribute("datasetsJson", gson.toJson(report.getChartDatasets()));
        request.setAttribute("typeLabels", BranchReportService.typeLabels());
        request.setAttribute("fromDate", range.getFrom().toString());
        request.setAttribute("toDate", range.getTo().toString());
        request.setAttribute("filterError", range.getError());
        ManagerShell.apply(request, user, isAdmin, report.getBranchPill(), branches, branchId,
                "branch-report", "/report/branch-report");
        request.getRequestDispatcher("/WEB-INF/view/report/branch-report-tuyet.jsp").forward(request, response);
    }
}
