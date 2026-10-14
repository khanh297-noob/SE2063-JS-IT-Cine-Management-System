package cinemanagement.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import cinemanagement.dao.BranchDAO;
import cinemanagement.dao.MovieDAO;
import cinemanagement.dao.RoomDAO;
import cinemanagement.model.OverlapResult;
import cinemanagement.model.Showtime;
import cinemanagement.model.User;
import cinemanagement.service.ShowtimeService;
import cinemanagement.service.ShowtimeService.ShowtimeInput;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DateTimeHelper;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Showtime Management (Branch Manager / Admin).
 * GET  /manager/showtimes                 danh sach theo ngay/phong
 * GET  /manager/showtimes/new | /edit?id  form tao / sua
 * GET  /manager/showtimes/check-overlap   AJAX kiem tra trung lich (JSON)
 * POST /manager/showtimes/save            tao / cap nhat
 * POST /manager/showtimes/assign-room     gan phong
 * POST /manager/showtimes/assign-movie    gan phim
 * POST /manager/showtimes/cancel          huy suat (hoan tien)
 * POST /manager/showtimes/delete          xoa (khi chua phat sinh ve)
 */
@WebServlet("/manager/showtimes/*")
public class ShowtimeController extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final ShowtimeService service = new ShowtimeService();
    private final BranchDAO branchDAO = new BranchDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        switch (pathInfo(req)) {
            case "", "/" -> list(req, resp, actor);
            case "/new" -> showForm(req, resp, actor, null);
            case "/edit" -> {
                try {
                    showForm(req, resp, actor, service.get(actor, Web.intParam(req, "id", -1)));
                } catch (BusinessException e) {
                    Web.flash(req, "error", e.getMessage());
                    Web.redirect(req, resp, "/manager/showtimes");
                }
            }
            case "/check-overlap" -> checkOverlap(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        String ip = Web.clientIp(req);
        int id = Web.intParam(req, "id", -1);
        String back = Web.trim(req.getParameter("back"));
        String redirect = back.startsWith("/manager/") ? back : "/manager/showtimes";
        try {
            switch (pathInfo(req)) {
                case "/save" -> {
                    ShowtimeInput in = readInput(req);
                    if (id > 0) {
                        service.update(actor, id, in, ip);
                        Web.flash(req, "success", "Cập nhật suất chiếu thành công. (MSG57)");
                    } else {
                        service.create(actor, in, ip);
                        Web.flash(req, "success", "Tạo suất chiếu thành công. (MSG24)");
                    }
                    redirect = "/manager/showtimes?date=" + in.date + "&branchId=" + branchOfRoom(in.roomId);
                }
                case "/assign-room" -> {
                    service.assignRoom(actor, id, Web.intParam(req, "roomId", -1), ip);
                    Web.flash(req, "success", "Đã gán phòng mới cho suất chiếu.");
                }
                case "/assign-movie" -> {
                    service.assignMovie(actor, id, Web.intParam(req, "movieId", -1), ip);
                    Web.flash(req, "success", "Đã gán phim mới cho suất chiếu (giờ kết thúc được tính lại).");
                }
                case "/cancel" -> {
                    int n = service.cancel(actor, id, ip);
                    Web.flash(req, "success", "Đã hủy suất chiếu. " + n + " đơn vé được hủy và hoàn tiền.");
                }
                case "/delete" -> {
                    service.delete(actor, id, ip);
                    Web.flash(req, "success", "Xóa suất chiếu thành công. (MSG05)");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            Web.redirect(req, resp, redirect);
        } catch (BusinessException e) {
            if ("/save".equals(pathInfo(req))) {
                // hien lai form voi du lieu da nhap
                req.setAttribute("error", e.getMessage());
                Showtime existing = null;
                if (id > 0) {
                    try {
                        existing = service.get(actor, id);
                    } catch (BusinessException ignored) {
                        // bo qua
                    }
                }
                showForm(req, resp, actor, existing);
            } else {
                Web.flash(req, "error", e.getMessage());
                Web.redirect(req, resp, redirect);
            }
        }
    }

    // ------------------------------------------------------------------

    private void list(HttpServletRequest req, HttpServletResponse resp, User actor) throws Exception {
        int branchId;
        try {
            branchId = service.resolveBranchId(actor, Web.intParam(req, "branchId"));
        } catch (BusinessException e) {
            req.setAttribute("errorMessage", e.getMessage());
            Web.forward(req, resp, "error");
            return;
        }
        LocalDate date = DateTimeHelper.parseDate(req.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }
        Integer roomId = Web.intParam(req, "roomId");
        req.setAttribute("branches", actor.isAdmin() ? branchDAO.findAll() : null);
        req.setAttribute("branch", branchDAO.findById(branchId));
        req.setAttribute("branchId", branchId);
        req.setAttribute("rooms", roomDAO.findByBranch(branchId));
        req.setAttribute("movies", movieDAO.findSchedulable());
        req.setAttribute("date", date.toString());
        req.setAttribute("prevDate", date.minusDays(1).toString());
        req.setAttribute("nextDate", date.plusDays(1).toString());
        req.setAttribute("dateLabel", DateTimeHelper.weekday(date) + ", " + DateTimeHelper.format(date));
        req.setAttribute("roomId", roomId);
        req.setAttribute("showtimes", service.list(branchId, date, roomId));
        String qs = req.getQueryString();
        req.setAttribute("back", "/manager/showtimes" + (qs == null ? "" : "?" + qs));
        Web.forward(req, resp, "manager/showtime-list");
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, User actor, Showtime st) throws Exception {
        int branchId;
        try {
            branchId = st != null ? st.getBranchId() : service.resolveBranchId(actor, Web.intParam(req, "branchId"));
        } catch (BusinessException e) {
            req.setAttribute("errorMessage", e.getMessage());
            Web.forward(req, resp, "error");
            return;
        }
        req.setAttribute("showtime", st);
        req.setAttribute("branch", branchDAO.findById(branchId));
        req.setAttribute("rooms", roomDAO.findByBranch(branchId));
        req.setAttribute("movies", movieDAO.findSchedulable());
        req.setAttribute("statuses", ShowtimeService.EDITABLE_STATUSES);
        if (st != null && req.getAttribute("error") == null) {
            req.setAttribute("soldCount", st.getSoldCount());
        }
        // gia tri mac dinh khi tao moi tu danh sach
        if (st == null && req.getParameter("date") != null && req.getParameter("startTime") == null) {
            req.setAttribute("defaultDate", req.getParameter("date"));
        }
        Web.forward(req, resp, "manager/showtime-form");
    }

    /** AJAX: tra ve gio ket thuc du kien + danh sach suat bi trung. */
    private void checkOverlap(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Integer roomId = Web.intParam(req, "roomId");
        Integer movieId = Web.intParam(req, "movieId");
        LocalDate date = DateTimeHelper.parseDate(req.getParameter("date"));
        LocalTime start = DateTimeHelper.parseTime(req.getParameter("startTime"));
        Integer excludeId = Web.intParam(req, "excludeId");
        if (roomId == null || movieId == null || date == null || start == null) {
            Web.json(resp, "{\"ready\":false}");
            return;
        }
        try {
            OverlapResult r = service.checkOverlap(roomId, movieId, date, start, excludeId);
            StringBuilder sb = new StringBuilder("{\"ready\":true,\"ok\":").append(r.isOk())
                    .append(",\"end\":").append(Web.js(r.getEndStr()))
                    .append(",\"nextDay\":").append(r.isNextDay())
                    .append(",\"buffer\":").append(r.getBufferMinutes())
                    .append(",\"conflicts\":[");
            for (int i = 0; i < r.getConflicts().size(); i++) {
                Showtime s = r.getConflicts().get(i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append("{\"movie\":").append(Web.js(s.getMovieTitle()))
                  .append(",\"start\":").append(Web.js(s.getStartStr()))
                  .append(",\"end\":").append(Web.js(s.getEndStr()))
                  .append(",\"date\":").append(Web.js(s.getDateStr())).append('}');
            }
            sb.append("]}");
            Web.json(resp, sb.toString());
        } catch (BusinessException e) {
            Web.json(resp, "{\"ready\":false,\"error\":" + Web.js(e.getMessage()) + "}");
        }
    }

    private ShowtimeInput readInput(HttpServletRequest req) throws BusinessException {
        ShowtimeInput in = new ShowtimeInput();
        Integer movieId = Web.intParam(req, "movieId");
        Integer roomId = Web.intParam(req, "roomId");
        if (movieId == null) {
            throw new BusinessException("Phim là bắt buộc. (MSG02)");
        }
        if (roomId == null) {
            throw new BusinessException("Phòng chiếu là bắt buộc. (MSG02)");
        }
        in.movieId = movieId;
        in.roomId = roomId;
        in.date = DateTimeHelper.parseDate(req.getParameter("date"));
        in.start = DateTimeHelper.parseTime(req.getParameter("startTime"));
        if (in.date == null || in.start == null) {
            throw new BusinessException("Ngày chiếu và giờ bắt đầu là bắt buộc. (MSG02)");
        }
        try {
            in.price = new BigDecimal(Web.trim(req.getParameter("basePrice")).replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new BusinessException("Giá vé không hợp lệ. (MSG07)");
        }
        in.status = req.getParameter("status");
        return in;
    }

    private int branchOfRoom(int roomId) throws Exception {
        var room = roomDAO.findById(roomId);
        return room == null ? 0 : room.getBranchId();
    }
}
