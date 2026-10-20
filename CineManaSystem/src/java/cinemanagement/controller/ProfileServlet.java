package cinemanagement.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Booking;
import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.service.ProfileService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/profile/*")
public class ProfileServlet extends HttpServlet {
    private ProfileService profileService;
    private BookingService bookingService;

    @Override
    public void init() throws ServletException {
        profileService = new ProfileService();
        bookingService = new BookingService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        try {
            switch (path) {
                case "", "/" -> {
                    reloadFormContext(request, user);
                    request.getRequestDispatcher("/WEB-INF/views/profile/profile-hiep.jsp").forward(request, response);
                }
                case "/history" -> {
                    String filter = request.getParameter("filter");
                    if (filter == null || !List.of("all", "upcoming", "past", "cancelled").contains(filter)) {
                        filter = "all";
                    }
                    List<Booking> all = bookingService.history(user);
                    List<Booking> shown = new ArrayList<>();
                    for (Booking b : all) {
                        boolean keep = switch (filter) {
                            case "upcoming" -> b.isUpcoming();
                            case "past" -> b.isConfirmed() && !b.isUpcoming();
                            case "cancelled" -> b.isCancelled();
                            default -> true;
                        };
                        if (keep) {
                            shown.add(b);
                        }
                    }
                    request.setAttribute("filter", filter);
                    request.setAttribute("bookings", shown);
                    request.setAttribute("totalCount", all.size());
                    request.getRequestDispatcher("/WEB-INF/views/profile/history-hiep.jsp").forward(request, response);
                }
                case "/deactivate" ->
                    request.getRequestDispatcher("/WEB-INF/views/profile/deactivate-hiep.jsp").forward(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            request.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải thông tin tài khoản: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error-hiep.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        String ip = Web.clientIp(request);
        try {
            switch (path) {
                case "/update" -> {
                    try {
                        profileService.updateProfile(user, request.getParameter("fullName"), request.getParameter("phone"), ip);
                        session.setAttribute("successMsg", "Cập nhật thành công. (MSG03)");
                        response.sendRedirect(request.getContextPath() + "/profile");
                    } catch (BusinessException e) {
                        request.setAttribute("errorMsg", e.getMessage());
                        reloadFormContext(request, user);
                        request.getRequestDispatcher("/WEB-INF/views/profile/profile-hiep.jsp").forward(request, response);
                    }
                }
                case "/password" -> {
                    try {
                        profileService.changePassword(user, request.getParameter("currentPassword"),
                                request.getParameter("newPassword"), request.getParameter("confirmPassword"), ip);
                        session.setAttribute("successMsg", "Đổi mật khẩu thành công. (MSG03)");
                        response.sendRedirect(request.getContextPath() + "/profile");
                    } catch (BusinessException e) {
                        request.setAttribute("errorMsg", e.getMessage());
                        reloadFormContext(request, user);
                        request.getRequestDispatcher("/WEB-INF/views/profile/profile-hiep.jsp").forward(request, response);
                    }
                }
                case "/deactivate" -> {
                    try {
                        profileService.deactivate(user, request.getParameter("password"),
                                "on".equals(request.getParameter("confirm")), ip);
                        session.invalidate();
                        request.getSession(true).setAttribute("successMsg",
                                "Tài khoản của bạn đã được vô hiệu hóa. Liên hệ quản trị viên nếu muốn kích hoạt lại.");
                        response.sendRedirect(request.getContextPath() + "/");
                    } catch (BusinessException e) {
                        request.setAttribute("errorMsg", e.getMessage());
                        request.getRequestDispatcher("/WEB-INF/views/profile/deactivate-hiep.jsp").forward(request, response);
                    }
                }
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            request.setAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error-hiep.jsp").forward(request, response);
        }
    }

    private void reloadFormContext(HttpServletRequest request, User user) throws Exception {
        request.setAttribute("profile", profileService.load(user.getUserId()));
    }
}
