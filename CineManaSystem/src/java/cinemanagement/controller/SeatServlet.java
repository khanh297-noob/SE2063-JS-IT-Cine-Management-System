package cinemanagement.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import cinemanagement.model.SeatRow;
import cinemanagement.model.Showtime;
import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.AppConfig;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/booking/seats", "/booking/seat-status", "/booking/hold"})
public class SeatServlet extends HttpServlet {
    private BookingService bookingService;

    @Override
    public void init() throws ServletException {
        bookingService = new BookingService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String backPath = "/home";
        try {
            String showtimeIdRaw = request.getParameter("showtimeId");
            if (showtimeIdRaw == null || showtimeIdRaw.isEmpty()) {
                session.setAttribute("errorMsg", "Vui lòng chọn một suất chiếu cụ thể để đặt vé.");
                response.sendRedirect(request.getContextPath() + backPath);
                return;
            }
            int showtimeId = Integer.parseInt(showtimeIdRaw);
            if (request.getServletPath().endsWith("/seat-status")) {
                writeSeatStatus(response, user, showtimeId);
                return;
            }
            Showtime showtime = bookingService.getBookableShowtime(user, showtimeId);
            reloadFormContext(request, user, showtime);
            request.getRequestDispatcher("/WEB-INF/views/booking/seats-hiep.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã suất chiếu trên đường dẫn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + backPath);
        } catch (BusinessException e) {
            session.setAttribute("errorMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + backPath);
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải sơ đồ ghế: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + backPath);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String showtimeIdRaw = request.getParameter("showtimeId");

        if (showtimeIdRaw == null || showtimeIdRaw.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        try {
            int showtimeId = Integer.parseInt(showtimeIdRaw);
            try {
                bookingService.holdSeats(user, showtimeId, Web.intParams(request, "seatIds"));
                response.sendRedirect(request.getContextPath() + "/booking/checkout?showtimeId=" + showtimeId);
            } catch (BusinessException e) {
                request.setAttribute("errorMsg", e.getMessage());
                reloadFormContext(request, user, bookingService.getBookableShowtime(user, showtimeId));
                request.getRequestDispatcher("/WEB-INF/views/booking/seats-hiep.jsp").forward(request, response);
            }
        } catch (BusinessException e) {
            session.setAttribute("errorMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/home");
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/booking/seats?showtimeId=" + showtimeIdRaw);
        }
    }

    private void reloadFormContext(HttpServletRequest request, User user, Showtime showtime) throws Exception {
        request.setAttribute("showtime", showtime);
        List<SeatRow> rows = bookingService.seatMap(user, showtime);
        request.setAttribute("rows", rows);
        request.setAttribute("seatCols", bookingService.seatColumns(rows));
        request.setAttribute("prices", bookingService.priceTable(rows));
        request.setAttribute("maxSeats", AppConfig.maxSeatsPerBooking());
        request.setAttribute("holdMinutes", AppConfig.seatHoldMinutes());
        request.setAttribute("hold", bookingService.getHold(user, showtime.getShowtimeId()));
        request.setAttribute("conflicts", bookingService.personalConflicts(user, showtime));
    }

    private void writeSeatStatus(HttpServletResponse response, User user, int showtimeId) throws Exception {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> e : bookingService.seatStates(user, showtimeId).entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            first = false;
        }
        Web.json(response, sb.append('}').toString());
    }
}
