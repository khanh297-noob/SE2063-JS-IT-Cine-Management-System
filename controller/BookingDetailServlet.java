package cinemanagement.controller;

import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.AppConfig;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Chi tiet don + E-ticket QR (Generate QR) + nut huy. */
@WebServlet("/booking/detail")
public class BookingDetailServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final BookingService service = new BookingService();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        try {
            req.setAttribute("booking", service.getBooking(actor, Web.intParam(req, "id", -1)));
            req.setAttribute("cutoffHours", AppConfig.cancelCutoffHours());
            Web.forward(req, resp, "booking/detail");
        } catch (BusinessException e) {
            Web.flash(req, "error", e.getMessage());
            Web.redirect(req, resp, actor.isCashier() ? "/pos/bookings" : actor.isCustomer() ? "/profile/history" : "/");
        }
    }
}
