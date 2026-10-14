package cinemanagement.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.dao.BranchDAO;
import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.model.ScheduleView;
import cinemanagement.model.Showtime;
import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DateTimeHelper;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * POS (Ticket Cashier): chon suat chieu cua chi nhanh minh de ban ve, tra cuu & huy don tai quay.
 * GET /pos            suat chieu dang mo ban
 * GET /pos/bookings?q= tra cuu don (ma don / SDT / email)
 */
@WebServlet(urlPatterns = {"/pos", "/pos/bookings"})
public class PosServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final BookingService bookingService = new BookingService();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        if (actor.getBranchId() == null) {
            req.setAttribute("errorMessage", "Tài khoản thu ngân chưa được gán chi nhánh.");
            Web.forward(req, resp, "error");
            return;
        }
        req.setAttribute("branch", branchDAO.findById(actor.getBranchId()));
        if (req.getServletPath().equals("/pos/bookings")) {
            String q = Web.trim(req.getParameter("q"));
            if (!q.isEmpty()) {
                try {
                    req.setAttribute("results", bookingService.searchForPos(actor, q));
                } catch (BusinessException e) {
                    req.setAttribute("error", e.getMessage());
                }
            }
            req.setAttribute("q", q);
            Web.forward(req, resp, "pos/bookings");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate date = DateTimeHelper.parseDate(req.getParameter("date"));
        if (date == null || date.isBefore(today)) {
            date = today;
        }
        List<ScheduleView.DayColumn> days = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            days.add(new ScheduleView.DayColumn(today.plusDays(i)));
        }
        List<Showtime> showtimes = new ArrayList<>();
        for (Showtime s : showtimeDAO.search(actor.getBranchId(), date, date, null, null, false)) {
            if (!s.isFinished()) {
                showtimes.add(s);
            }
        }
        req.setAttribute("days", days);
        req.setAttribute("selectedDate", date.toString());
        req.setAttribute("showtimes", showtimes);
        Web.forward(req, resp, "pos/index");
    }
}
