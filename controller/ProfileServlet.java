package cinemanagement.controller;

import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Booking;
import cinemanagement.model.User;
import cinemanagement.service.BookingService;
import cinemanagement.service.ProfileService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Customer Profile.
 * GET  /profile               thong tin ca nhan + doi mat khau
 * POST /profile/update        cap nhat ho ten, SDT
 * POST /profile/password      doi mat khau
 * GET  /profile/history       lich su dat ve (filter=all|upcoming|past|cancelled)
 * GET  /profile/deactivate    trang xac nhan
 * POST /profile/deactivate    tu vo hieu hoa tai khoan
 */
@WebServlet("/profile/*")
public class ProfileServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final ProfileService profileService = new ProfileService();
    private final BookingService bookingService = new BookingService();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        switch (pathInfo(req)) {
            case "", "/" -> {
                req.setAttribute("profile", profileService.load(actor.getUserId()));
                Web.forward(req, resp, "profile/profile");
            }
            case "/history" -> history(req, resp, actor);
            case "/deactivate" -> Web.forward(req, resp, "profile/deactivate");
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        String ip = Web.clientIp(req);
        String path = pathInfo(req);
        try {
            switch (path) {
                case "/update" -> {
                    profileService.updateProfile(actor, req.getParameter("fullName"), req.getParameter("phone"), ip);
                    Web.flash(req, "success", "Cập nhật thành công. (MSG03)");
                    Web.redirect(req, resp, "/profile");
                }
                case "/password" -> {
                    profileService.changePassword(actor, req.getParameter("currentPassword"),
                            req.getParameter("newPassword"), req.getParameter("confirmPassword"), ip);
                    Web.flash(req, "success", "Đổi mật khẩu thành công. (MSG03)");
                    Web.redirect(req, resp, "/profile");
                }
                case "/deactivate" -> {
                    profileService.deactivate(actor, req.getParameter("password"),
                            "on".equals(req.getParameter("confirm")), ip);
                    HttpSession s = req.getSession(false);
                    if (s != null) {
                        s.invalidate();
                    }
                    Web.flash(req, "info", "Tài khoản của bạn đã được vô hiệu hóa. Liên hệ quản trị viên nếu muốn kích hoạt lại.");
                    Web.redirect(req, resp, "/");
                }
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (BusinessException e) {
            Web.flash(req, "error", e.getMessage());
            Web.redirect(req, resp, "/deactivate".equals(path) ? "/profile/deactivate" : "/profile");
        }
    }

    private void history(HttpServletRequest req, HttpServletResponse resp, User actor) throws Exception {
        String filter = req.getParameter("filter");
        if (filter == null || !List.of("all", "upcoming", "past", "cancelled").contains(filter)) {
            filter = "all";
        }
        List<Booking> all = bookingService.history(actor);
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
        req.setAttribute("filter", filter);
        req.setAttribute("bookings", shown);
        req.setAttribute("totalCount", all.size());
        Web.forward(req, resp, "profile/history");
    }
}
