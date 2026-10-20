package cinemanagement.controller;

import java.io.IOException;

import cinemanagement.model.HoldSummary;
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

@WebServlet("/booking/checkout")
public class CheckoutServlet extends HttpServlet {
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
        try {
            String showtimeIdRaw = request.getParameter("showtimeId");
            if (showtimeIdRaw == null || showtimeIdRaw.isEmpty()) {
                session.setAttribute("errorMsg", "Vui lòng chọn suất chiếu và ghế trước khi thanh toán.");
                response.sendRedirect(request.getContextPath() + "/home");
                return;
            }
            int showtimeId = Integer.parseInt(showtimeIdRaw);
            HoldSummary hold = bookingService.getHold(user, showtimeId);
            if (hold == null) {
                session.setAttribute("errorMsg", "Thời gian giữ ghế đã hết hoặc bạn chưa chọn ghế. Vui lòng chọn lại.");
                response.sendRedirect(request.getContextPath() + "/booking/seats?showtimeId=" + showtimeId);
                return;
            }
            reloadFormContext(request, user, hold);
            request.getRequestDispatcher("/WEB-INF/views/booking/checkout-hiep.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã suất chiếu trên đường dẫn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/home");
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải thông tin thanh toán: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String showtimeIdRaw = request.getParameter("showtimeId");
        String action = request.getParameter("action");
        String method = request.getParameter("method");

        if (showtimeIdRaw == null || showtimeIdRaw.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        try {
            int showtimeId = Integer.parseInt(showtimeIdRaw);
            if ("release".equals(action)) {
                bookingService.releaseHold(user, showtimeId);
                session.setAttribute("successMsg", "Đã nhả ghế đang giữ.");
                response.sendRedirect(request.getContextPath() + "/booking/seats?showtimeId=" + showtimeId);
                return;
            }
            try {
                int bookingId = bookingService.confirm(user, showtimeId, method, Web.clientIp(request));
                session.setAttribute("successMsg", "Thanh toán thành công. Đặt vé thành công! (MSG29, MSG28)");
                response.sendRedirect(request.getContextPath() + "/booking/detail?id=" + bookingId);
            } catch (BusinessException e) {
                HoldSummary hold = bookingService.getHold(user, showtimeId);
                if (hold == null) {
                    session.setAttribute("errorMsg", e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/booking/seats?showtimeId=" + showtimeId);
                    return;
                }
                request.setAttribute("errorMsg", e.getMessage());
                reloadFormContext(request, user, hold);
                request.getRequestDispatcher("/WEB-INF/views/booking/checkout-hiep.jsp").forward(request, response);
            }
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã suất chiếu không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/home");
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/booking/checkout?showtimeId=" + showtimeIdRaw);
        }
    }

    private void reloadFormContext(HttpServletRequest request, User user, HoldSummary hold) {
        request.setAttribute("hold", hold);
        request.setAttribute("methods", BookingService.PAYMENT_METHODS);
    }
}
