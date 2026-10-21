package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;
import model.WorkShift;
import service.ShiftService;

/**
 * Két tiền: số tiền mặt phải có trong két của ca hiện tại.
 */
@WebServlet(name = "PosCashBalanceServlet", urlPatterns = {"/pos/cash-balance"})
public class PosCashBalanceServlet extends HttpServlet {

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
        WorkShift shift = shiftService.getActiveShift(user);
        if (shift == null) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "Bạn chưa bắt đầu ca làm việc.");
            response.sendRedirect(request.getContextPath() + "/pos/shift");
            return;
        }
        request.setAttribute("shift", shift);
        request.setAttribute("bal", shiftService.getCashBalance(user, shift));
        request.getRequestDispatcher("/WEB-INF/view/pos/cash-balance-nam.jsp").forward(request, response);
    }
}
