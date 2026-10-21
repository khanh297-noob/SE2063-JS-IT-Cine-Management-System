package controller.payment;

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
 * Trang kết quả thanh toán của 1 đơn.
 * state: SUCCESS | PENDING (đang chờ, trang tự tải lại) | LATE (tiền về sau khi hết giờ giữ ghế) | FAILED | EXPIRED
 */
@WebServlet(name = "PaymentResultServlet", urlPatterns = {"/payment/result"})
public class PaymentResultServlet extends HttpServlet {

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
        int bookingId;
        try {
            bookingId = Integer.parseInt(request.getParameter("bookingId"));
        } catch (Exception e) {
            bookingId = -1;
        }

        Booking b = paymentService.getBooking(bookingId);
        boolean isAdmin = "Administrator".equals(user.getRole()) || "Branch Management".equals(user.getRole());
        if (b == null || (b.getUserId() != user.getUserId() && !isAdmin)) {
            request.setAttribute("errorMsg", "No search results.");
            request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
            return;
        }

        String state;
        if ("Confirmed".equals(b.getStatus()) && "Success".equals(b.getPaymentStatus())) {
            state = "SUCCESS";
        } else if ("Success".equals(b.getPaymentStatus())) {
            state = "LATE";
        } else if ("Pending".equals(b.getStatus()) && "Pending".equals(b.getPaymentStatus())) {
            state = "PENDING";
        } else if ("Failed".equals(b.getPaymentStatus())) {
            state = "FAILED";
        } else {
            state = "EXPIRED";
        }
        request.setAttribute("b", b);
        request.setAttribute("state", state);
        request.getRequestDispatcher("/WEB-INF/view/payment/result-nam.jsp").forward(request, response);
    }
}
