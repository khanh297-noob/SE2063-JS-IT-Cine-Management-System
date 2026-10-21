package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.Booking;
import model.User;
import service.PaymentService;

/**
 * Trang Checkout: xem tóm tắt đơn, áp voucher, chọn cách thanh toán.
 * Phần đặt vé (của nhóm) tạo Booking 'Pending' rồi chuyển tới /payment/checkout?bookingId={id}
 *  GET  : hiển thị
 *  POST : action=apply (áp voucher) | action=remove (gỡ voucher)
 */
@WebServlet(name = "CheckoutServlet", urlPatterns = {"/payment/checkout"})
public class CheckoutServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int bookingId = parseInt(request.getParameter("bookingId"));

        Booking b = paymentService.getBooking(bookingId);

        // Đơn đã thanh toán rồi -> sang trang kết quả
        if (b != null && b.getUserId() == user.getUserId() && "Confirmed".equals(b.getStatus())) {
            response.sendRedirect(request.getContextPath() + "/payment/result?bookingId=" + bookingId);
            return;
        }
        try {
            paymentService.checkCanPay(b, user);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMsg", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
            return;
        }
        request.setAttribute("b", b);
        request.setAttribute("secondsLeft", paymentService.getSecondsLeft(b));
        request.getRequestDispatcher("/WEB-INF/view/payment/checkout-nam.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int bookingId = parseInt(request.getParameter("bookingId"));
        String action = request.getParameter("action");
        String voucherCode = request.getParameter("voucherCode");
        voucherCode = voucherCode == null ? "" : voucherCode.trim().toUpperCase();

        try {
            if ("remove".equals(action)) {
                paymentService.removeVoucher(bookingId, user);
                session.setAttribute("flashMsg", "Voucher removed.");
            } else {
                paymentService.applyVoucher(bookingId, user, voucherCode);
                session.setAttribute("flashMsg", "Promotion applied successfully.");
            }
            session.setAttribute("flashType", "success");
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/payment/checkout?bookingId=" + bookingId);
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
}
