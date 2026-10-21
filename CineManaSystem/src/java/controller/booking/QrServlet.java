package controller.booking;

import dao.booking.TicketDAO;
import model.User;
import model.booking.Ticket;
import service.booking.OnlineBookingService;
import util.BusinessException;
import util.PermissionUtil;
import util.QRCodeGenerator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /ticket/qr?id={TicketID}&size= : ảnh PNG mã QR của vé (nội dung = TicketCode, cửa soát vé quét mã này).
 * Chỉ người xem được đơn mới lấy được QR, và chỉ vé còn hiệu lực của đơn đã thanh toán.
 */
@WebServlet("/ticket/qr")
public class QrServlet extends HttpServlet {

    private final TicketDAO ticketDAO = new TicketDAO();
    private final OnlineBookingService bookingService = new OnlineBookingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = PermissionUtil.getCurrentUser(req);
        int ticketId;
        int size;
        try {
            ticketId = Integer.parseInt(req.getParameter("id"));
            String sizeRaw = req.getParameter("size");
            size = sizeRaw == null || sizeRaw.isEmpty() ? 240 : Integer.parseInt(sizeRaw);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        size = Math.max(120, Math.min(600, size));
        Ticket t = ticketDAO.getById(ticketId);
        if (t == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            bookingService.getBooking(user, t.getBookingId());    // kiểm tra quyền xem đơn
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!"Confirmed".equals(t.getBookingStatus()) || !"Valid".equals(t.getStatus())) {
            resp.sendError(HttpServletResponse.SC_GONE, "Ve khong con hieu luc");
            return;
        }
        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "private, no-store");
        QRCodeGenerator.writePng(t.getTicketCode(), size, resp.getOutputStream());
    }
}
