package controller.booking;

import controller.home.CustomerBaseServlet;
import model.User;
import model.booking.HoldSummary;
import service.booking.OnlineBookingService;
import util.BusinessException;
import util.PermissionUtil;
import util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/booking/fnb")
public class FnbServlet extends CustomerBaseServlet {

    private final OnlineBookingService bookingService = new OnlineBookingService();

    static String cartKey(int showtimeId) {
        return "fnbCart_" + showtimeId;
    }

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        Integer showtimeId = intParam(req, "showtimeId");
        if (showtimeId == null) {
            redirect(req, resp, "/booking");
            return;
        }
        HoldSummary hold = bookingService.getHold(user, showtimeId);
        if (hold == null) {
            flashError(req, "Thời gian giữ ghế đã hết hoặc bạn chưa chọn ghế. Vui lòng chọn lại.");
            redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
            return;
        }
        prepareLayout(req);
        req.setAttribute("hold", hold);
        req.setAttribute("fnbMenu", bookingService.fnbMenu(hold.getShowtime().getBranchId(), cartOf(req.getSession(), showtimeId)));
        req.setAttribute("maxQty", OnlineBookingService.MAX_FNB_QUANTITY);
        render(req, resp, "booking/fnb-hiep");
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        HttpSession session = req.getSession();
        Integer showtimeId = intParam(req, "showtimeId");
        if (showtimeId == null) {
            redirect(req, resp, "/booking");
            return;
        }
        String action = action(req, "pay");
        if ("release".equals(action)) {
            bookingService.releaseHold(user, showtimeId);
            session.removeAttribute(cartKey(showtimeId));
            flashSuccess(req, "Đã nhả ghế đang giữ.");
            redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
            return;
        }

        Map<Integer, Integer> requested = new LinkedHashMap<>();
        if (!"skip".equals(action)) {
            Enumeration<String> names = req.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (name.startsWith("qty_")) {
                    Integer itemId = intOrNull(name.substring(4));
                    Integer qty = intParam(req, name);
                    if (itemId != null) requested.put(itemId, qty == null ? 0 : qty);
                }
            }
        }
        try {
            int bookingId = bookingService.createPendingBooking(user, showtimeId, requested, WebUtil.clientIp(req));
            session.removeAttribute(cartKey(showtimeId));
            redirect(req, resp, "/payment/checkout?bookingId=" + bookingId);
        } catch (BusinessException e) {
            session.setAttribute(cartKey(showtimeId), requested);
            flashError(req, e.getMessage());
            if (bookingService.getHold(user, showtimeId) == null) {
                redirect(req, resp, "/booking/seats?showtimeId=" + showtimeId);
            } else {
                redirect(req, resp, "/booking/fnb?showtimeId=" + showtimeId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> cartOf(HttpSession session, int showtimeId) {
        return (Map<Integer, Integer>) session.getAttribute(cartKey(showtimeId));
    }

    private Integer intOrNull(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
