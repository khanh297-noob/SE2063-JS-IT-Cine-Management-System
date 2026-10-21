package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import model.User;
import model.WorkShift;
import service.ShiftService;

/**
 * Vào ca / kết ca của thu ngân và nhân viên soát vé.
 *  GET  /pos/shift              : ca đang làm + giờ làm tháng này
 *  POST /pos/shift action=start : vào ca (chọn rạp + tiền đầu ca)
 *  POST /pos/shift action=end   : kết ca (nhập tiền đếm thực tế)
 */
@WebServlet(name = "PosShiftServlet", urlPatterns = {"/pos/shift"})
public class PosShiftServlet extends HttpServlet {

    private ShiftService shiftService;

    @Override
    public void init() throws ServletException {
        shiftService = new ShiftService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        boolean isCashier = "Ticket Cashier".equals(user.getRole());

        WorkShift shift = shiftService.getActiveShift(user);
        if (shift != null && isCashier) {
            request.setAttribute("balance", shiftService.getCashBalance(user, shift));
        }

        // Các ca trong tháng này + tổng giờ
        List<WorkShift> shifts = shiftService.getShifts(user.getUserId(), LocalDate.now().withDayOfMonth(1), LocalDate.now());
        double hours = 0;
        for (WorkShift s : shifts) {
            hours += s.getHours();
        }

        request.setAttribute("shift", shift);
        request.setAttribute("shifts", shifts);
        request.setAttribute("monthHours", Math.round(hours * 100) / 100.0);
        request.setAttribute("branches", shiftService.getBranches());
        request.setAttribute("isCashier", isCashier);
        request.getRequestDispatcher("/WEB-INF/view/pos/shift-nam.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        String action = request.getParameter("action");

        try {
            if ("start".equals(action)) {
                int branchId = Integer.parseInt(request.getParameter("branchId"));
                BigDecimal openingFloat = parseMoney(request.getParameter("openingFloat"));
                shiftService.startShift(user, branchId, openingFloat, request.getRemoteAddr());
                session.setAttribute("flashType", "success");
                session.setAttribute("flashMsg", "Bắt đầu ca làm việc thành công.");
                // Thu ngân -> màn bán vé, soát vé -> màn soát vé
                if ("Ticket Cashier".equals(user.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/pos");
                } else {
                    response.sendRedirect(request.getContextPath() + "/checker");
                }
                return;
            } else {
                String msg = shiftService.endShift(user, parseMoney(request.getParameter("countedCash")),
                        request.getRemoteAddr());
                session.setAttribute("flashType", "success");
                session.setAttribute("flashMsg", msg);
            }
        } catch (NumberFormatException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "The Branch is required.");
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/pos/shift");
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.trim().isEmpty()) {
            return BigDecimal.ZERO;     // soát vé không nhập tiền -> 0
        }
        try {
            return new BigDecimal(s.trim());   // ô <input type="number"> gửi dạng 500000 hoặc 500000.00
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
