package cinemanagement.service;

import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Objects;

import cinemanagement.dao.TicketDAO;
import cinemanagement.model.Ticket;
import cinemanagement.model.User;
import cinemanagement.util.QRCodeGenerator;

public class TicketService {
    private final TicketDAO ticketDAO = new TicketDAO();

    public Ticket getViewableTicket(User actor, int ticketId) throws SQLException {
        Ticket t = ticketDAO.findById(ticketId);
        boolean canSee = t != null && (actor.isAdmin() || t.getOwnerUserId() == actor.getUserId()
                || (!actor.isCustomer() && Objects.equals(actor.getBranchId(), t.getBranchId())));
        return canSee ? t : null;
    }

    public boolean isQrValid(Ticket t) {
        return "Confirmed".equals(t.getBookingStatus()) && t.isValid();
    }

    public void writeQrPng(Ticket t, int size, OutputStream out) throws Exception {
        QRCodeGenerator.writePng(t.getTicketCode(), size, out);
    }
}
