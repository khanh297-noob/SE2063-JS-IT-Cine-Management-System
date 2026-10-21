package controller.management;

import dao.BranchDAO;
import dao.PaymentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import model.Payment;
import model.User;
import service.PaymentService;

/**
 * Quản lý thanh toán (Admin / Quản lý chi nhánh):
 *  GET  /admin/payments                   : danh sách + bộ lọc
 *  GET  /admin/payments?action=detail&id= : chi tiết 1 giao dịch
 *  POST /admin/payments (id, newStatus)   : xác nhận giao dịch Pending -> Success / Failed
 *                                          (đối chiếu với trang quản trị VNPay)
 */
@WebServlet(name = "ManagePaymentServlet", urlPatterns = {"/admin/payments"})
public class ManagePaymentServlet extends HttpServlet {

    private static final int PAGE_SIZE = 15;
    private PaymentService paymentService;
    private PaymentDAO paymentDAO;
    private BranchDAO branchDAO;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
        paymentDAO = new PaymentDAO();
        branchDAO = new BranchDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // ----- Chi tiết 1 giao dịch -----
        if ("detail".equals(request.getParameter("action"))) {
            Payment p = paymentService.getPayment(parseInt(request.getParameter("id"), -1));
            if (p == null) {
                request.setAttribute("errorMsg", "No search results.");
                request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
                return;
            }
            request.setAttribute("p", p);
            request.setAttribute("b", paymentService.getBooking(p.getBookingId()));
            request.getRequestDispatcher("/WEB-INF/view/admin/payment-detail-nam.jsp").forward(request, response);
            return;
        }

        // ----- Danh sách + bộ lọc -----
        String q = getParam(request, "q");
        String method = getParam(request, "method");
        String status = getParam(request, "status");
        String from = getParam(request, "from");
        String to = getParam(request, "to");
        int branchId = parseInt(request.getParameter("branchId"), 0);

        if (request.getParameter("from") == null && request.getParameter("to") == null) {
            // Lần đầu mở trang: mặc định tháng hiện tại
            from = LocalDate.now().withDayOfMonth(1).toString();
            to = LocalDate.now().toString();
        }
        if (!from.isEmpty() && !to.isEmpty() && from.compareTo(to) > 0) {
            request.setAttribute("dateError",
                    "The selected date range is invalid. Please check the start and end dates.");
            from = "";
            to = "";
        }

        int total = paymentDAO.count(q, method, status, from, to, branchId);
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = parseInt(request.getParameter("page"), 1);
        if (page < 1 || page > pages) {
            page = 1;
        }

        request.setAttribute("payments", paymentDAO.search(q, method, status, from, to, branchId, page, PAGE_SIZE));
        request.setAttribute("totalRecords", total);
        request.setAttribute("totalSuccess", paymentDAO.sumSuccess(q, method, status, from, to, branchId));
        request.setAttribute("page", page);
        request.setAttribute("pages", pages);
        request.setAttribute("branches", branchDAO.getAll());
        request.setAttribute("q", q);
        request.setAttribute("method", method);
        request.setAttribute("status", status);
        request.setAttribute("branchId", branchId);
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.getRequestDispatcher("/WEB-INF/view/admin/payments-nam.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User admin = (User) session.getAttribute("User");
        int id = parseInt(request.getParameter("id"), -1);
        String newStatus = request.getParameter("newStatus");

        try {
            paymentService.reconcile(id, newStatus, admin, request.getRemoteAddr());
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMsg", "Update successfully.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/payments?action=detail&id=" + id);
    }

    private String getParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
