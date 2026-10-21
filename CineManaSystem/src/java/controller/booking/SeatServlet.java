package controller.booking;

import controller.home.CustomerBaseServlet;
import model.User;
import model.booking.SeatRow;
import model.movie.Showtime;
import service.booking.OnlineBookingService;
import util.BusinessException;
import util.PermissionUtil;
import util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Bước 2: chọn ghế.
 *  GET  /booking/seats?showtimeId=        sơ đồ ghế (giá theo loại ghế, tự cập nhật mỗi 10 giây)
 *  GET  /booking/seat-status?showtimeId=  AJAX: trạng thái ghế
 *  POST /booking/hold                     giữ ghế đã chọn rồi sang bước đồ ăn & nước
 */
@WebServlet(urlPatterns = {"/booking/seats", "/booking/seat-status", "/booking/hold"})
public class SeatServlet extends CustomerBaseServlet {

    private final OnlineBookingService bookingService = new OnlineBookingService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        Integer showtimeId = intParam(req, "showtimeId");
        if (showtimeId == null) {
            flashError(req, "Vui lòng chọn một suất chiếu cụ thể để đặt vé.");
            redirect(req, resp, "/booking");
            return;
        }
        if ("/booking/seat-status".equals(req.getServletPath())) {
            writeSeatStatus(resp, user, showtimeId);
            return;
        }
        Showtime showtime;
        try {
            showtime = bookingService.getBookableShowtime(showtimeId);
        } catch (BusinessException e) {
            flashError(req, e.getMessage());
            redirect(req, resp, "/booking");
            return;
        }
        prepareLayout(req);
        req.setAttribute("showtime", showtime);
        List<SeatRow> rows = bookingService.seatMap(user, showtime);
        req.setAttribute("rows", rows);
        req.setAttribute("seatCols", bookingService.seatColumns(rows));
        req.setAttribute("prices", bookingService.priceTable(rows));
        req.setAttribute("maxSeats", OnlineBookingService.MAX_SEATS);
        req.setAttribute("holdMinutes", OnlineBookingService.holdMinutes());
        req.setAttribute("hold", bookingService.getHold(user, showtimeId));
        req.setAttribute("conflicts", bookingService.personalConflicts(user, showtime));
        req.setAttribute("pendingBookingId", bookingService.pendingBookingId(user, showtimeId));
        render(req, resp, "booking/seats-hiep");
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        Integer showtimeId = intParam(req, "showtimeId");
        if (showtimeId == null) {
            redirect(req, resp, "/booking");
            return;
        }
        try {
            bookingService.holdSeats(user, showtimeId, WebUtil.intParams(req, "seatIds"));
            redirect(req, resp, "/booking/fnb?showtimeId=" + showtimeId);
        } catch (BusinessException e) {
            flashError(req, e.getMessage());
            redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
        }
    }

    private void writeSeatStatus(HttpServletResponse resp, User user, int showtimeId)
            throws IOException, SQLException, BusinessException {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> e : bookingService.seatStates(user, showtimeId).entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            first = false;
        }
        WebUtil.json(resp, sb.append('}').toString());
    }
}
