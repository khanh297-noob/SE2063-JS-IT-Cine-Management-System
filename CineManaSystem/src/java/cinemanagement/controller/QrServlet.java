package cinemanagement.controller;

import java.io.IOException;

import cinemanagement.model.Ticket;
import cinemanagement.model.User;
import cinemanagement.service.TicketService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ticket/qr")
public class QrServlet extends HttpServlet {
    private TicketService ticketService;

    @Override
    public void init() throws ServletException {
        ticketService = new TicketService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession(false).getAttribute("user");
        try {
            String ticketIdRaw = request.getParameter("id");
            String sizeRaw = request.getParameter("size");
            if (ticketIdRaw == null || ticketIdRaw.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            int ticketId = Integer.parseInt(ticketIdRaw);
            int size = (sizeRaw == null || sizeRaw.isEmpty()) ? 240 : Integer.parseInt(sizeRaw);
            size = Math.max(120, Math.min(600, size));
            Ticket ticket = ticketService.getViewableTicket(user, ticketId);
            if (ticket == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!ticketService.isQrValid(ticket)) {
                response.sendError(HttpServletResponse.SC_GONE, "Ve khong con hieu luc");
                return;
            }
            response.setContentType("image/png");
            response.setHeader("Cache-Control", "private, no-store");
            ticketService.writeQrPng(ticket, size, response.getOutputStream());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            getServletContext().log("Loi tao QR", e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
