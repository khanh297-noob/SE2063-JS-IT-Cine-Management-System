package controller.pos;

import dao.pos.AuditLogDAO;
import model.User;
import model.booking.Booking;
import service.pos.PosService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * In vé giấy (có QR) + hóa đơn. Thêm &reprint=1 để in lại (chỉ ghi log, không tạo vé mới).
 */
@WebServlet(name = "PosPrintServlet", urlPatterns = {"/pos/print"})
public class PosPrintServlet extends HttpServlet {

    private PosService posService;
    private AuditLogDAO auditLogDAO;

    @Override
    public void init() throws ServletException {
        posService = new PosService();
        auditLogDAO = new AuditLogDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        int bookingId;
        try {
            bookingId = Integer.parseInt(request.getParameter("bookingId"));
        } catch (Exception e) {
            bookingId = -1;
        }
        boolean isReprint = request.getParameter("reprint") != null;

        Booking b = posService.getBooking(bookingId);
        String error = null;
        if (b == null) {
            error = "No search results.";
        } else if ("Ticket Cashier".equals(user.getRole()) && b.getUserId() != user.getUserId()
                && !"Customer".equals(b.getCustomerRole())) {
            // Thu ngân chỉ in đơn của mình (hoặc đơn online của khách)
            error = "You do not have permission to perform this action.";
        } else if (!"Confirmed".equals(b.getStatus())) {
            error = "Chỉ in vé cho đơn đã thanh toán (Confirmed).";
        } else if (isReprint && b.isAllCheckedIn()) {
            error = "Vé đã check-in, không thể in lại.";
        }
        if (error != null) {
            request.setAttribute("errorMsg", error);
            request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
            return;
        }

        if (isReprint) {
            auditLogDAO.insert(user.getUserId(), "REPRINT|booking=" + bookingId, request.getRemoteAddr());
        }
        request.setAttribute("b", b);
        request.setAttribute("isReprint", isReprint);
        request.getRequestDispatcher("/WEB-INF/view/pos/print-nam.jsp").forward(request, response);
    }
}
