package cinemanagement.controller;

import java.util.Objects;

import cinemanagement.dao.TicketDAO;
import cinemanagement.model.Ticket;
import cinemanagement.model.User;
import cinemanagement.util.QRCodeGenerator;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Generate QR: anh PNG chua TicketCode, chi cho ve con hieu luc cua don da xac nhan. */
@WebServlet("/ticket/qr")
public class QrServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final TicketDAO ticketDAO = new TicketDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        Ticket t = ticketDAO.findById(Web.intParam(req, "id", -1));
        boolean canSee = t != null && (actor.isAdmin() || t.getOwnerUserId() == actor.getUserId()
                || (!actor.isCustomer() && Objects.equals(actor.getBranchId(), t.getBranchId())));
        if (!canSee) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!"Confirmed".equals(t.getBookingStatus()) || !t.isValid()) {
            resp.sendError(HttpServletResponse.SC_GONE, "Ve khong con hieu luc");
            return;
        }
        int size = Math.max(120, Math.min(600, Web.intParam(req, "size", 240)));
        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "private, no-store");
        QRCodeGenerator.writePng(t.getTicketCode(), size, resp.getOutputStream());
    }
}
