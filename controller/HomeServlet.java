package cinemanagement.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cinemanagement.dao.BranchDAO;
import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.model.Branch;
import cinemanagement.model.MovieSchedule;
import cinemanagement.model.ScheduleView;
import cinemanagement.model.Showtime;
import cinemanagement.util.DateTimeHelper;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Lich chieu theo chi nhanh & ngay (SRS 3.3 - View Branch Showtimes). Guest xem duoc, dat ve can dang nhap. */
@WebServlet(urlPatterns = {"", "/home"})
public class HomeServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final BranchDAO branchDAO = new BranchDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        List<Branch> branches = branchDAO.findAll();
        Integer branchId = Web.intParam(req, "branchId");
        if (branchId == null && !branches.isEmpty()) {
            branchId = branches.get(0).getBranchId();
        }
        LocalDate today = LocalDate.now();
        LocalDate date = DateTimeHelper.parseDate(req.getParameter("date"));
        if (date == null || date.isBefore(today) || date.isAfter(today.plusDays(6))) {
            date = today;
        }
        List<ScheduleView.DayColumn> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(new ScheduleView.DayColumn(today.plusDays(i)));
        }

        Map<Integer, MovieSchedule> groups = new LinkedHashMap<>();
        if (branchId != null) {
            for (Showtime s : showtimeDAO.search(branchId, date, date, null, null, false)) {
                if ("Scheduled".equals(s.getStatus()) || s.isFinished()) {
                    continue;   // chua mo ban hoac da chieu xong thi khong hien
                }
                groups.computeIfAbsent(s.getMovieId(), k -> new MovieSchedule(s)).getShowtimes().add(s);
            }
        }
        req.setAttribute("branches", branches);
        req.setAttribute("branchId", branchId);
        req.setAttribute("days", days);
        req.setAttribute("selectedDate", date.toString());
        req.setAttribute("movies", new ArrayList<>(groups.values()));
        Web.forward(req, resp, "home");
    }
}
