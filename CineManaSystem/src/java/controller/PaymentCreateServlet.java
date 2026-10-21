package controller;

import model.Booking;
import model.User;
import service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * API-01: POST /api/v1/payment/create
 * Nút "Xác nhận & Thanh toán" ở trang Checkout gửi về đây (bookingId + paymentMethod).
 * Tạo giao dịch Pending rồi chuyển khách sang cổng thanh toán (VNPay hoặc MoMo sandbox).
 */
@WebServlet(name = "PaymentCreateServlet", urlPatterns = {"/api/v1/payment/create"})
public class PaymentCreateServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        String method = request.getParameter("paymentMethod");
        int bookingId;
        try {
            bookingId = Integer.parseInt(request.getParameter("bookingId"));
        } catch (Exception e) {
            bookingId = -1;
        }

        Booking b = paymentService.getBooking(bookingId);
        try {
            paymentService.checkCanPay(b, user);
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/payment/checkout?bookingId=" + bookingId);
            return;
        }

        // Đơn 0đ (voucher giảm hết) -> xác nhận luôn
        if (b.getTotalAmount().signum() == 0) {
            paymentService.payFreeBooking(b);
            response.sendRedirect(request.getContextPath() + "/payment/result?bookingId=" + bookingId);
            return;
        }

        // Link gốc của web, vd http://localhost:8080/CineManagement
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + request.getContextPath();
        try {
            if ("VNPay".equals(method)) {
                String ip = request.getRemoteAddr();
                if (ip.equals("0:0:0:0:0:0:0:1")) {
                    ip = "127.0.0.1";   // IPv6 localhost -> IPv4 cho VNPay
                }
                String url = paymentService.createVnpayUrl(b, baseUrl + "/payment/vnpay-return", ip);
                response.sendRedirect(url);
            } else if ("MoMo".equals(method)) {
                String url = paymentService.createMomoUrl(b, baseUrl + "/payment/momo-return",
                        baseUrl + "/api/v1/payment/webhook/momo");
                response.sendRedirect(url);
            } else {
                throw new IllegalArgumentException("Invalid Payment method format.");
            }
        } catch (IllegalArgumentException e) {
            // Chọn sai phương thức / không gọi được cổng thanh toán -> quay lại trang checkout
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/payment/checkout?bookingId=" + bookingId);
        }
    }
}
