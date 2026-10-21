package controller.booking;

import controller.home.CustomerBaseServlet;
import model.User;
import model.booking.Booking;
import service.booking.OnlineBookingService;
import util.BusinessException;
import util.FormatUtil;
import util.PermissionUtil;
import util.SessionUtil;
import util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý hồ sơ khách (phần vé): bổ sung cho trang Hồ sơ /customer/profile của module Auth.
 *  GET  /customer/bookings?filter=     Vé của tôi: all | upcoming | past | cancelled
 *  GET  /booking/detail?id=            chi tiết đơn + QR từng vé
 *  POST /booking/cancel                khách tự huỷ đơn (trước giờ chiếu 2 tiếng, hoàn 100%)
 *  GET/POST /customer/deactivate       khách tự vô hiệu hoá tài khoản
 */
@WebServlet(urlPatterns = {"/customer/bookings", "/booking/detail", "/booking/cancel", "/customer/deactivate"})
public class MyBookingServlet extends CustomerBaseServlet {

    private static final List<String> FILTERS = List.of("all", "upcoming", "past", "cancelled");
    private final OnlineBookingService bookingService = new OnlineBookingService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        prepareLayout(req);
        switch (req.getServletPath()) {
            case "/customer/bookings" -> showHistory(req, resp, user);
            case "/booking/detail" -> {
                Booking b;
                try {
                    b = bookingService.getBooking(user, requiredId(req, "id"));
                } catch (BusinessException e) {
                    flashError(req, e.getMessage());
                    redirect(req, resp, "/customer/bookings");
                    return;
                }
                req.setAttribute("b", b);
                req.setAttribute("cancelBlockReason", bookingService.cancelBlockReason(user, b));
                req.setAttribute("cutoffHours", OnlineBookingService.CANCEL_CUTOFF_HOURS);
                render(req, resp, "booking/detail-hiep");
            }
            case "/customer/deactivate" -> render(req, resp, "booking/deactivate-hiep");
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        String ip = WebUtil.clientIp(req);
        switch (req.getServletPath()) {
            case "/booking/cancel" -> {
                int bookingId = requiredId(req, "id");
                try {
                    BigDecimal refund = bookingService.cancel(user, bookingId, ip);
                    flashSuccess(req, "Hủy đơn thành công. Số tiền hoàn: " + FormatUtil.vnd(refund));
                } catch (BusinessException e) {
                    flashError(req, e.getMessage());
                }
                redirect(req, resp, "/booking/detail?id=" + bookingId);
            }
            case "/customer/deactivate" -> {
                try {
                    bookingService.deactivate(user, req.getParameter("password"), "on".equals(req.getParameter("confirm")), ip);
                } catch (BusinessException e) {
                    flashError(req, e.getMessage());
                    redirect(req, resp, "/customer/deactivate");
                    return;
                }
                SessionUtil.logout(req);
                req.getSession(true).setAttribute("loginNotice",
                        "Tài khoản của bạn đã được vô hiệu hóa. Liên hệ quản trị viên nếu muốn kích hoạt lại.");
                redirect(req, resp, "/login");
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showHistory(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String filter = str(req, "filter");
        if (filter == null || !FILTERS.contains(filter)) filter = "all";
        List<Booking> all = bookingService.history(user);
        List<Booking> shown = new ArrayList<>();
        for (Booking b : all) {
            boolean keep = switch (filter) {
                case "upcoming" -> "Confirmed".equals(b.getStatus()) && !b.isShowFinished();
                case "past" -> "Confirmed".equals(b.getStatus()) && b.isShowFinished();
                case "cancelled" -> "Cancelled".equals(b.getStatus());
                default -> true;
            };
            if (keep) shown.add(b);
        }
        req.setAttribute("filter", filter);
        req.setAttribute("bookings", shown);
        req.setAttribute("totalCount", all.size());
        render(req, resp, "booking/history-hiep");
    }
}
