package cinemanagement.controller;

import java.util.Map;

import cinemanagement.model.HoldSummary;
import cinemanagement.model.Showtime;
import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.AppConfig;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Select Seat + Seat Holding.
 * GET  /booking/seats?showtimeId=     so do ghe
 * GET  /booking/seat-status?showtimeId= AJAX trang thai ghe (JSON)
 * POST /booking/hold                  giu ghe da chon -> chuyen sang /booking/checkout
 */
@WebServlet(urlPatterns = {"/booking/seats", "/booking/seat-status", "/booking/hold"})
public class SeatServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final BookingService service = new BookingService();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        int showtimeId = Web.intParam(req, "showtimeId", -1);
        if (req.getServletPath().endsWith("/seat-status")) {
            seatStatus(resp, actor, showtimeId);
            return;
        }
        Showtime st;
        try {
            st = service.getBookableShowtime(actor, showtimeId);
        } catch (BusinessException e) {
            Web.flash(req, "error", e.getMessage());
            Web.redirect(req, resp, actor.isCashier() ? "/pos" : "/");
            return;
        }
        HoldSummary hold = service.getHold(actor, showtimeId);
        req.setAttribute("showtime", st);
        req.setAttribute("rows", service.seatMap(actor, st));
        req.setAttribute("prices", service.priceTable(st));
        req.setAttribute("maxSeats", AppConfig.maxSeatsPerBooking());
        req.setAttribute("holdMinutes", AppConfig.seatHoldMinutes());
        req.setAttribute("hold", hold);
        req.setAttribute("conflicts", service.personalConflicts(actor, st));
        Web.forward(req, resp, "booking/seats");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        int showtimeId = Web.intParam(req, "showtimeId", -1);
        try {
            service.holdSeats(actor, showtimeId, Web.intParams(req, "seatIds"));
            Web.redirect(req, resp, "/booking/checkout?showtimeId=" + showtimeId);
        } catch (BusinessException e) {
            Web.flash(req, "error", e.getMessage());
            Web.redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
        }
    }

    private void seatStatus(HttpServletResponse resp, User actor, int showtimeId) throws Exception {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> e : service.seatStates(actor, showtimeId).entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            first = false;
        }
        Web.json(resp, sb.append('}').toString());
    }
}
