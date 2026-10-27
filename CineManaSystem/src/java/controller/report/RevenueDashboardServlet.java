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
import service.ReportAccessService;
import service.RevenueDashboardService;
import service.RevenueDashboardService.RevenueDashboard;
import util.DateRange;
import util.ManagerShell;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Dashboard doanh thu vé + F&B, có biểu đồ theo tuần và 3 bảng chi tiết (phim, loại ghế, thanh toán).
 */
@WebServlet(name = "revenueDashboard", urlPatterns = {"/report/revenue-dashboard"})
public class RevenueDashboardServlet extends HttpServlet {

    private ReportDao reportDao;
    private ReportAccessService accessService;
    private RevenueDashboardService dashboardService;
    private BranchReportService reportService;

    @Override
    public void init() throws ServletException {
        reportDao = new ReportDao();
        accessService = new ReportAccessService(reportDao);
        dashboardService = new RevenueDashboardService(reportDao);
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

        //đọc bộ lọc (khoảng ngày mặc định là tháng hiện tại)
        DateRange range = DateRange.fromRequest(request);
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));

        // Bước 3: xác định chi nhánh được xem
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, isAdmin, branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        //lấy dữ liệu
        List<Branch> branches = reportDao.getBranches(accessService.allowedBranchIds(user, isAdmin));
        RevenueDashboard dashboard = dashboardService.build(branchScope, range.getFrom(), range.getTo());

        //gửi sang JSP (dữ liệu biểu đồ chuyển sang JSON cho Chart.js)
        Gson gson = new Gson();
        request.setAttribute("dashboard", dashboard);
        request.setAttribute("labelsJson", gson.toJson(dashboard.getWeekLabels()));
        request.setAttribute("ticketJson", gson.toJson(dashboard.getWeeklyTicketRevenue()));
        request.setAttribute("fnbJson", gson.toJson(dashboard.getWeeklyFnbRevenue()));
        request.setAttribute("fromDate", range.getFrom().toString());
        request.setAttribute("toDate", range.getTo().toString());
        request.setAttribute("filterError", range.getError());

        // Tab bảng chi tiết đang hiển thị (mặc định: theo phim)
        String breakdown = request.getParameter("breakdown");
        if (breakdown == null || breakdown.trim().isEmpty()) {
            breakdown = "movie";
        }
        request.setAttribute("selectedBreakdown", breakdown);

        ManagerShell.apply(request, user, isAdmin, reportService.scopePill(branchScope), branches, branchId,
                "revenue", "/report/revenue-dashboard");
        request.getRequestDispatcher("/WEB-INF/view/report/revenue-dashboard-tuyet.jsp").forward(request, response);
    }
}
