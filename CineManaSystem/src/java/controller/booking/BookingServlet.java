package controller.booking;

import controller.home.CustomerBaseServlet;
import model.movie.CustomerMovieDetailDTO;
import service.booking.OnlineBookingService;
import util.BusinessException;
import util.DateTimeHelper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WebServlet("/booking")
public class BookingServlet extends CustomerBaseServlet {

    private final OnlineBookingService bookingService = new OnlineBookingService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        prepareLayout(req);
        Integer movieId = intParam(req, "movieId");
        if (movieId == null) {
            req.setAttribute("movies", customerService.listShowing(null, null, null));
            render(req, resp, "booking/movies-hiep");
            return;
        }

        CustomerMovieDetailDTO detail = customerService.getDetail(movieId);
        Set<LocalDate> available = bookingService.datesWithShowtimes(movieId);
        LocalDate today = LocalDate.now();
        LocalDate date = dateParam(req, "date");
        if (date == null || date.isBefore(today)) {
            date = available.isEmpty() ? today : available.iterator().next();
        }

        Map<String, String[]> days = new LinkedHashMap<>();
        List<String> enabled = new ArrayList<>();
        for (int i = 0; i < OnlineBookingService.SHOWTIME_DAYS; i++) {
            LocalDate d = today.plusDays(i);
            String label = i == 0 ? "Hôm nay" : DateTimeHelper.weekday(d);
            days.put(d.toString(), new String[] {label, d.format(DateTimeHelper.SHORT_DATE)});
            if (available.contains(d)) enabled.add(d.toString());
        }
        req.setAttribute("detail", detail);
        req.setAttribute("movie", detail.getMovie());
        req.setAttribute("days", days);
        req.setAttribute("enabledDays", enabled);
        req.setAttribute("date", date.toString());
        req.setAttribute("dateLabel", DateTimeHelper.weekday(date) + ", " + DateTimeHelper.format(date));
        req.setAttribute("showtimesByBranch", bookingService.showtimesOfMovie(movieId, date));
        render(req, resp, "booking/showtimes-hiep");
    }
}
