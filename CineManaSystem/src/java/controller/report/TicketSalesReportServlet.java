package controller.report;

import dao.ReportDao;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Branch;
import model.Movie;
import dao.ReportDao.SalesSummary;
import dao.ReportDao.TicketReportItem;
import service.BranchReportService;
import service.ReportAccessService;
import util.ManagerShell;
import util.PermissionUtil;

@WebServlet(name = "ticketSalesReport", urlPatterns = {
    "/report/ticket-sales",
    "/admin/report/ticket-sales",
    "/staff/report/ticket-sales"
})
public class TicketSalesReportServlet extends HttpServlet {

    private final ReportDao reportDao = new ReportDao();
    private final ReportAccessService accessService = new ReportAccessService(reportDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiem tra quyen: Nguoi dung phai la Admin hoac Quan ly rap
        if (!PermissionUtil.canViewSalesReport(request)) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // 2. Nhan cac tham so loc tu request
        String branchIdParam = request.getParameter("branchId");
        String movieIdParam = request.getParameter("movieId");
        String fromDate = request.getParameter("fromDate");
        String toDate = request.getParameter("toDate");

        // Parse ma chi nhanh (neu co)
        Integer branchId = null;
        if (branchIdParam != null && !branchIdParam.trim().isEmpty()) {
            try {
                branchId = Integer.parseInt(branchIdParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Parse ma phim (neu co)
        Integer movieId = null;
        if (movieIdParam != null && !movieIdParam.trim().isEmpty()) {
            try {
                movieId = Integer.parseInt(movieIdParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        // 3. Xac dinh danh sach chi nhanh duoc phep xem theo phan quyen
        List<Integer> branchScope;
        try {
            branchScope = accessService.resolveBranchScope(
                    PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request), branchId);
        } catch (SecurityException ex) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        }

        // 4. Lay danh sach chi nhanh va phim cho hop thoai chon
        List<Branch> branches = reportDao.getBranches(
                accessService.allowedBranchIds(
                        PermissionUtil.getCurrentUser(request), PermissionUtil.isAdmin(request)));
        List<Movie> movies = reportDao.getAllMovies();

        // 5. Truy van du lieu danh sach ve va so lieu tong hop
        List<TicketReportItem> reportItems = reportDao.getTicketSalesReport(branchScope, movieId, fromDate, toDate);
        SalesSummary summary = reportDao.getSalesSummary(branchScope, movieId, fromDate, toDate);

        // 6. Lay du lieu thong ke doanh thu theo ngay va theo phim
        List<Object[]> dailyRevenue = reportDao.getDailyRevenueChart(branchScope, movieId, fromDate, toDate);
        List<Object[]> movieRevenue = reportDao.getRevenueByMovieChart(branchScope, fromDate, toDate);

        // 7. Gui du lieu sang JSP qua request attributes
        request.setAttribute("branches", branches);
        request.setAttribute("movies", movies);
        request.setAttribute("reportItems", reportItems);
        request.setAttribute("summary", summary);
        request.setAttribute("dailyRevenue", dailyRevenue);
        request.setAttribute("movieRevenue", movieRevenue);

        // 8. Giu lai cac gia tri nguoi dung da chon tren form tim kiem
        request.setAttribute("selectedBranchId", branchId);
        request.setAttribute("selectedMovieId", movieId);
        request.setAttribute("selectedFromDate", fromDate);
        request.setAttribute("selectedToDate", toDate);

        // 9. Thiet lap thong tin chung cho Navbar va Sidebar
        ManagerShell.apply(request,
                PermissionUtil.getCurrentUser(request),
                PermissionUtil.isAdmin(request),
                new BranchReportService(reportDao).scopePill(branchScope),
                branches,
                branchId,
                "tickets",
                "/report/ticket-sales",
                accessService.isBranchScopingConfigured());

        // 10. Chuyen tiep den trang sales-report.jsp
        request.getRequestDispatcher("/view/report/sales-report.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
