package cinemanagement.controller;

import cinemanagement.model.HoldSummary;
import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Holding Seat Screen + Booking Ticket (SRS 4.2, 4.3, 5.1 POS).
 * GET  /booking/checkout?showtimeId=   tom tat ghe dang giu + dong ho dem nguoc
 * POST action=confirm                  thanh toan & tao don
 * POST action=release                  nha ghe, quay lai chon ghe
 */
@WebServlet("/booking/checkout")
public class CheckoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final BookingService service = new BookingService();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        int showtimeId = Web.intParam(req, "showtimeId", -1);
        HoldSummary hold = service.getHold(actor, showtimeId);
        if (hold == null) {
            Web.flash(req, "error", "Thời gian giữ ghế đã hết hoặc bạn chưa chọn ghế. Vui lòng chọn lại.");
            Web.redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
            return;
        }
        req.setAttribute("hold", hold);
        req.setAttribute("methods", actor.isCashier() ? BookingService.POS_METHODS : BookingService.ONLINE_METHODS);
        Web.forward(req, resp, "booking/checkout");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        int showtimeId = Web.intParam(req, "showtimeId", -1);
        if ("release".equals(req.getParameter("action"))) {
            service.releaseHold(actor, showtimeId);
            Web.flash(req, "info", "Đã nhả ghế đang giữ.");
            Web.redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
            return;
        }
        try {
            int bookingId = service.confirm(actor, showtimeId, req.getParameter("method"),
                    req.getParameter("customerKey"), Web.clientIp(req));
            Web.flash(req, "success", "Thanh toán thành công. Đặt vé thành công! (MSG29, MSG28)");
            Web.redirect(req, resp, "/booking/detail?id=" + bookingId);
        } catch (BusinessException e) {
            Web.flash(req, "error", e.getMessage());
            Web.redirect(req, resp, "/booking/checkout?showtimeId=" + showtimeId);
        }
    }
}
