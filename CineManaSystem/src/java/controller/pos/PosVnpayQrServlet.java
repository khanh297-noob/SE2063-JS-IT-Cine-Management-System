package controller.pos;

import model.User;
import model.booking.Booking;
import service.payment.PaymentService;
import service.pos.PosService;
import service.pos.ShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Màn hình thanh toán VNPay QR tại quầy.
 *  GET  /pos/vnpay-qr?bookingId=      : hiện mã QR (link thanh toán VNPay sandbox).
 *                                       Trang tự tải lại mỗi 5 giây; mỗi lần tải server hỏi VNPay
 *                                       khách đã trả chưa -> trả rồi thì chuyển sang trang in vé.
 *  POST /pos/vnpay-qr                 : thu ngân hủy (khách đổi ý) -> hủy đơn, nhả ghế.
 */
@WebServlet(name = "PosVnpayQrServlet", urlPatterns = {"/pos/vnpay-qr"})
public class PosVnpayQrServlet extends HttpServlet {

    private PosService posService;
    private ShiftService shiftService;
    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        posService = new PosService();
        shiftService = new ShiftService();
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int bookingId = parseInt(request.getParameter("bookingId"));

        Booking b = posService.getBooking(bookingId);
        if (b == null || b.getUserId() != user.getUserId() || !"VNPay".equals(b.getPaymentMethod())) {
            request.setAttribute("errorMsg", "No search results.");
            request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
            return;
        }

        // Hỏi VNPay khách đã trả tiền chưa
        String status = posService.checkVnpayPayment(b);
        if ("PAID".equals(status)) {
            session.removeAttribute("vnpayPayUrl_" + bookingId);
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMsg", "Khách đã thanh toán VNPay thành công. Booking created successfully.");
            response.sendRedirect(request.getContextPath() + "/pos/print?bookingId=" + bookingId + "&auto=1");
            return;
        }
        if ("FAILED".equals(status)) {
            session.removeAttribute("vnpayPayUrl_" + bookingId);
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "Thanh toán VNPay thất bại hoặc hết thời gian. Đơn đã hủy, ghế đã được nhả.");
            response.sendRedirect(request.getContextPath() + "/pos");
            return;
        }

        // Còn chờ -> hiện QR
        request.setAttribute("b", b);
        request.setAttribute("payUrl", session.getAttribute("vnpayPayUrl_" + bookingId));
        request.setAttribute("secondsLeft", paymentService.getSecondsLeft(b));
        request.setAttribute("shift", shiftService.getActiveShift(user));
        request.getRequestDispatcher("/WEB-INF/view/pos/vnpay-qr-nam.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int bookingId = parseInt(request.getParameter("bookingId"));
        try {
            posService.cancelVnpayPayment(user, bookingId, request.getRemoteAddr());
            session.removeAttribute("vnpayPayUrl_" + bookingId);
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMsg", "Đã hủy thanh toán VNPay, ghế đã được nhả.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/pos");
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
}
