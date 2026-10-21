package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import model.Booking;
import model.User;
import service.PosService;
import service.ShiftService;

/**
 * Giao dịch tại quầy của thu ngân.
 *  GET  /pos/transactions?date=&status=     : danh sách
 *  GET  /pos/transactions?action=detail&id= : chi tiết (in lại, hủy & hoàn tiền)
 *  POST /pos/transactions (id, reason, email + mật khẩu quản lý) : hủy đơn & hoàn tiền
 */
@WebServlet(name = "PosTransactionServlet", urlPatterns = {"/pos/transactions"})
public class PosTransactionServlet extends HttpServlet {

    private PosService posService;
    private ShiftService shiftService;

    @Override
    public void init() throws ServletException {
        posService = new PosService();
        shiftService = new ShiftService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        request.setAttribute("shift", shiftService.getActiveShift(user));

        // ----- Chi tiết 1 đơn -----
        if ("detail".equals(request.getParameter("action"))) {
            Booking b = posService.getBooking(parseInt(request.getParameter("id")));
            if (b == null || b.getUserId() != user.getUserId()) {
                request.setAttribute("errorMsg", "No search results.");
                request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
                return;
            }
            request.setAttribute("b", b);
            request.getRequestDispatcher("/WEB-INF/view/pos/transaction-detail-nam.jsp").forward(request, response);
            return;
        }

        // ----- Danh sách theo ngày -----
        LocalDate date;
        try {
            date = LocalDate.parse(request.getParameter("date"));
        } catch (Exception e) {
            date = LocalDate.now();
        }
        String status = request.getParameter("status");
        if (status == null) {
            status = "";
        }
        List<Booking> list = posService.getTransactions(user, date, status);
        BigDecimal sum = BigDecimal.ZERO;
        for (Booking b : list) {
            if ("Confirmed".equals(b.getStatus())) {
                sum = sum.add(b.getTotalAmount());
            }
        }
        request.setAttribute("list", list);
        request.setAttribute("sum", sum);
        request.setAttribute("date", date.toString());
        request.setAttribute("status", status);
        request.getRequestDispatcher("/WEB-INF/view/pos/transactions-nam.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int id = parseInt(request.getParameter("id"));
        String reason = request.getParameter("reason");

        try {
            posService.cancelCounterBooking(user, id,
                    request.getParameter("supervisorEmail"),
                    request.getParameter("supervisorPassword"),
                    reason == null ? "" : reason.trim(),
                    request.getRemoteAddr());
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMsg", "Booking cancelled successfully. Refund processed successfully.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/pos/transactions?action=detail&id=" + id);
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
}
