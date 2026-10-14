package cinemanagement.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Booking Cancellation - Customer (online) va Ticket Cashier (tai quay). */
@WebServlet("/booking/cancel")
public class CancelBookingServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final BookingService service = new BookingService();

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        int id = Web.intParam(req, "id", -1);
        try {
            BigDecimal refund = service.cancel(actor, id, Web.clientIp(req));
            String money = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(refund);
            Web.flash(req, "success", "Hủy đơn thành công. Số tiền hoàn: " + money + " ₫ (MSG32, MSG51)");
        } catch (BusinessException e) {
            Web.flash(req, "error", e.getMessage());
        }
        Web.redirect(req, resp, "/booking/detail?id=" + id);
    }
}
