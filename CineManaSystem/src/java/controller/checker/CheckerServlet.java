package controller.checker;

import model.Branch;
import model.User;
import model.booking.Ticket;
import model.pos.WorkShift;
import service.checker.TicketService;
import service.pos.ShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Màn hình soát vé.
 *  GET  /checker                              : màn hình (quét QR bằng camera / nhập tay)
 *  POST /checker action=search, keyword=...   : mã vé TK... -> soát vé + check-in
 *                                               mã đơn / SĐT / email -> tra cứu đơn
 *  POST /checker action=checkin, ticketId=... : check-in 1 vé từ kết quả tra cứu
 */
@WebServlet(name = "CheckerServlet", urlPatterns = {"/checker"})
public class CheckerServlet extends HttpServlet {

    private ShiftService shiftService;
    private TicketService ticketService;

    @Override
    public void init() throws ServletException {
        shiftService = new ShiftService();
        ticketService = new TicketService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        WorkShift shift = shiftService.getActiveShift(user);
        if (shift == null && "Ticket Checker".equals(user.getRole())) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "Bạn cần bắt đầu ca làm việc trước khi soát vé.");
            response.sendRedirect(request.getContextPath() + "/pos/shift");
            return;
        }
        showPage(request, response, shift);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        WorkShift shift = shiftService.getActiveShift(user);
        if (shift == null && "Ticket Checker".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/pos/shift");
            return;
        }
        int branchId = getGateBranch(request, shift);
        String action = request.getParameter("action");
        String keyword = request.getParameter("keyword");
        keyword = keyword == null ? "" : keyword.trim();

        if ("checkin".equals(action)) {
            // Check-in 1 vé từ danh sách tra cứu
            int ticketId;
            try {
                ticketId = Integer.parseInt(request.getParameter("ticketId"));
            } catch (Exception e) {
                ticketId = -1;
            }
            checkIn(request, ticketService.getById(ticketId), branchId, user);
            if (!keyword.isEmpty()) {
                request.setAttribute("bookings", ticketService.lookup(keyword, branchId));   // cập nhật lại danh sách
            }
        } else if (keyword.toUpperCase().startsWith("TK")) {
            // Mã vé (nội dung QR) -> soát vé luôn
            checkIn(request, ticketService.getByCode(keyword), branchId, user);
        } else if (!keyword.isEmpty()) {
            // Mã đơn / SĐT / email -> tra cứu
            request.setAttribute("bookings", ticketService.lookup(keyword, branchId));
            request.setAttribute("searched", true);
        }
        request.setAttribute("keyword", keyword);
        showPage(request, response, shift);
    }

    // Soát 1 vé và đưa kết quả ra JSP: cleared = true (hợp lệ) / false (từ chối)
    private void checkIn(HttpServletRequest request, Ticket t, int branchId, User user) {
        try {
            ticketService.checkIn(t, branchId, user, request.getRemoteAddr());
            request.setAttribute("cleared", true);
            request.setAttribute("resultMsg", "MỜI KHÁCH HÀNG VÀO RẠP");
        } catch (IllegalArgumentException e) {
            request.setAttribute("cleared", false);
            request.setAttribute("resultMsg", e.getMessage());
        }
        request.setAttribute("ticket", t);
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, WorkShift shift)
            throws ServletException, IOException {
        request.setAttribute("shift", shift);
        request.setAttribute("branchId", getGateBranch(request, shift));
        request.setAttribute("branches", shiftService.getBranches());
        request.getRequestDispatcher("/WEB-INF/view/checker/checker-nam.jsp").forward(request, response);
    }

    /**
     * Rạp đang soát vé: nhân viên trong ca -> rạp của ca;
     * quản lý/admin (không có ca) -> rạp chọn ở dropdown (lưu trong session).
     */
    private int getGateBranch(HttpServletRequest request, WorkShift shift) {
        if (shift != null) {
            return shift.getBranchId();
        }
        HttpSession session = request.getSession();
        String param = request.getParameter("branchId");
        if (param != null) {
            try {
                session.setAttribute("gateBranchId", Integer.parseInt(param));
            } catch (NumberFormatException e) {
                // bỏ qua giá trị sai
            }
        }
        Integer saved = (Integer) session.getAttribute("gateBranchId");
        if (saved != null) {
            return saved;
        }
        List<Branch> branches = shiftService.getBranches();
        return branches.isEmpty() ? 0 : branches.get(0).getBranchId();
    }
}
