package controller.report;

import dao.ReportDao;
import dao.ReportDao.SalesSummary;
import dao.ReportDao.TicketReportItem;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import model.Branch;
import model.Movie;
import model.User;
import service.BranchReportService;
import service.ReportAccessService;
import util.ManagerShell;
import util.ParamUtil;
import util.PermissionUtil;

/**
 * Báo cáo bán vé: danh sách vé đã bán, tổng hợp và 2 biểu đồ (theo ngày, theo phim).
 */
@WebServlet(name = "ticketSalesReport", urlPatterns = {
    "/report/ticket-sales",
    "/admin/report/ticket-sales",
    "/staff/report/ticket-sales"
})
public class TicketSalesReportServlet extends HttpServlet {

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
        // Bước 1: kiểm tra quyền (Admin hoặc Quản lý chi nhánh)
        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }
        User user = PermissionUtil.getCurrentUser(request);
        boolean isAdmin = PermissionUtil.isAdmin(request);

        // Bước 2: đọc bộ lọc
        Integer branchId = ParamUtil.parseIntOrNull(request.getParameter("branchId"));
        Integer movieId = ParamUtil.parseIntOrNull(request.getParameter("movieId"));
        String fromDate = request.getParameter("fromDate");
        String toDate = request.getParameter("toDate");

        // Bước 3: xác định chi nhánh được xem
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(user, isAdmin, branchId);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // Bước 4: lấy danh sách chi nhánh, phim cho ô chọn
        List<Branch> branches = reportDao.getBranches(accessService.allowedBranchIds(user, isAdmin));
        List<Movie> movies = reportDao.getAllMovies();

        // Bước 5: lấy dữ liệu báo cáo
        List<TicketReportItem> reportItems = reportDao.getTicketSalesReport(branchScope, movieId, fromDate, toDate);
        SalesSummary summary = reportDao.getSalesSummary(branchScope, movieId, fromDate, toDate);
        List<Object[]> dailyRevenue = reportDao.getDailyRevenueChart(branchScope, movieId, fromDate, toDate);
        List<Object[]> movieRevenue = reportDao.getRevenueByMovieChart(branchScope, fromDate, toDate);

        // Bước 6: gửi sang JSP
        request.setAttribute("branches", branches);
        request.setAttribute("movies", movies);
        request.setAttribute("reportItems", reportItems);
        request.setAttribute("summary", summary);
        request.setAttribute("dailyRevenue", dailyRevenue);
        request.setAttribute("movieRevenue", movieRevenue);

        // Giữ lại giá trị người dùng đã chọn trên form lọc
        request.setAttribute("selectedBranchId", branchId);
        request.setAttribute("selectedMovieId", movieId);
        request.setAttribute("selectedFromDate", fromDate);
        request.setAttribute("selectedToDate", toDate);

        ManagerShell.apply(request, user, isAdmin, reportService.scopePill(branchScope), branches, branchId,
                "tickets", "/report/ticket-sales", accessService.isBranchScopingConfigured());
        request.getRequestDispatcher("/WEB-INF/view/report/sales-report-tuyet.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
