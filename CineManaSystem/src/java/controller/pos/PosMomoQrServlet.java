package controller.pos;

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
import service.PosService;
import service.ShiftService;

/**
 * Màn hình thanh toán MoMo QR tại quầy.
 *  GET  /pos/momo-qr?bookingId=      : hiện mã QR (link thanh toán MoMo sandbox) cho khách quét.
 *                                      Trang tự tải lại mỗi 5 giây; mỗi lần tải server hỏi MoMo
 *                                      khách đã trả chưa -> trả rồi thì chuyển sang trang in vé.
 *  POST /pos/momo-qr action=cancel    : thu ngân hủy (khách đổi ý) -> hủy đơn, nhả ghế.
 */
@WebServlet(name = "PosMomoQrServlet", urlPatterns = {"/pos/momo-qr"})
public class PosMomoQrServlet extends HttpServlet {

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
        if (b == null || b.getUserId() != user.getUserId() || !"MoMo".equals(b.getPaymentMethod())) {
            request.setAttribute("errorMsg", "No search results.");
            request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
            return;
        }

        // Hỏi MoMo khách đã trả tiền chưa
        String status = posService.checkMomoPayment(b);
        if ("PAID".equals(status)) {
            session.removeAttribute("momoPayUrl_" + bookingId);
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMsg", "Khách đã thanh toán MoMo thành công. Booking created successfully.");
            response.sendRedirect(request.getContextPath() + "/pos/print?bookingId=" + bookingId + "&auto=1");
            return;
        }
        if ("FAILED".equals(status)) {
            session.removeAttribute("momoPayUrl_" + bookingId);
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "Thanh toán MoMo thất bại hoặc hết thời gian. Đơn đã hủy, ghế đã được nhả.");
            response.sendRedirect(request.getContextPath() + "/pos");
            return;
        }

        // Còn chờ -> hiện QR
        request.setAttribute("b", b);
        request.setAttribute("payUrl", session.getAttribute("momoPayUrl_" + bookingId));
        request.setAttribute("secondsLeft", paymentService.getSecondsLeft(b));
        request.setAttribute("shift", shiftService.getActiveShift(user));
        request.getRequestDispatcher("/WEB-INF/view/pos/momo-qr-nam.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int bookingId = parseInt(request.getParameter("bookingId"));
        try {
            posService.cancelMomoPayment(user, bookingId, request.getRemoteAddr());
            session.removeAttribute("momoPayUrl_" + bookingId);
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMsg", "Đã hủy thanh toán MoMo, ghế đã được nhả.");
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
