package cinemanagement.controller;

import java.io.IOException;

import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.util.AppConfig;
import cinemanagement.util.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/booking/detail")
public class BookingDetailServlet extends HttpServlet {
    private BookingService bookingService;

    @Override
    public void init() throws ServletException {
        bookingService = new BookingService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String backPath = user.isCustomer() ? "/profile/history" : "/";
        try {
            String bookingIdRaw = request.getParameter("id");
            if (bookingIdRaw == null || bookingIdRaw.isEmpty()) {
                session.setAttribute("errorMsg", "Vui lòng chọn một đơn vé cụ thể để xem.");
                response.sendRedirect(request.getContextPath() + backPath);
                return;
            }
            int bookingId = Integer.parseInt(bookingIdRaw);
            request.setAttribute("booking", bookingService.getBooking(user, bookingId));
            request.setAttribute("cutoffHours", AppConfig.cancelCutoffHours());
            request.getRequestDispatcher("/WEB-INF/views/booking/detail-hiep.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã đơn trên đường dẫn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + backPath);
        } catch (BusinessException e) {
            session.setAttribute("errorMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + backPath);
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải đơn vé: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + backPath);
        }
    }
}
