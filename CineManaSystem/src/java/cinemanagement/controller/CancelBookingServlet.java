package cinemanagement.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/booking/cancel")
public class CancelBookingServlet extends HttpServlet {
    private BookingService bookingService;

    @Override
    public void init() throws ServletException {
        bookingService = new BookingService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String bookingIdRaw = request.getParameter("id");

        if (bookingIdRaw == null || bookingIdRaw.isEmpty()) {
            session.setAttribute("errorMsg", "Vui lòng chọn đơn cần hủy.");
            response.sendRedirect(request.getContextPath() + "/profile/history");
            return;
        }
        try {
            int bookingId = Integer.parseInt(bookingIdRaw);
            try {
                BigDecimal refund = bookingService.cancel(user, bookingId, Web.clientIp(request));
                String money = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(refund);
                session.setAttribute("successMsg", "Hủy đơn thành công. Số tiền hoàn: " + money + " ₫ (MSG32, MSG51)");
            } catch (BusinessException e) {
                session.setAttribute("errorMsg", e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/booking/detail?id=" + bookingId);
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã đơn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/profile/history");
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Lỗi hệ thống khi hủy đơn: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/booking/detail?id=" + bookingIdRaw);
        }
    }
}
