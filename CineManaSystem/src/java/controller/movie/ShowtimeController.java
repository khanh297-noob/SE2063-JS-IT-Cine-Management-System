package controller.movie;

import controller.BaseServlet;
import model.User;
import model.movie.OverlapResult;
import model.movie.Showtime;
import service.movie.ShowtimePriceCalculator;
import service.movie.ShowtimeService;
import service.movie.ShowtimeService.ShowtimeInput;
import util.BusinessException;
import util.DateTimeHelper;
import util.PermissionUtil;
import util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Quản lý suất chiếu (Admin + Quản lý chi nhánh, AuthFilter chặn quyền theo /manager/*).
 *  GET  /manager/showtimes                 danh sách theo ngày / phòng
 *  GET  /manager/showtimes/new | /edit     form tạo / sửa
 *  GET  /manager/showtimes/check-overlap   AJAX: giờ kết thúc + trùng lịch + giá tự tính
 *  POST /manager/showtimes/save | assign-room | assign-movie | cancel | delete
 */
@WebServlet("/manager/showtimes/*")
public class ShowtimeController extends BaseServlet {

    private static final String LIST = "/manager/showtimes";
    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "", "/" -> showList(req, resp, user);
            case "/new" -> {
                prepareForm(req, user, null);
                render(req, resp, "showtime/form-hiep");
            }
            case "/edit" -> {
                Showtime showtime = showtimeService.get(user, requiredId(req, "id"));
                prepareForm(req, user, showtime);
                render(req, resp, "showtime/form-hiep");
            }
            case "/check-overlap" -> checkOverlap(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String ip = WebUtil.clientIp(req);
        String back = str(req, "back");
        String redirectTo = back != null && back.startsWith("/manager/") ? back : LIST;
        Integer id = intParam(req, "id");

        switch (path) {
            case "/save" -> {
                try {
                    ShowtimeInput in = readInput(req);
                    if (id != null) {
                        showtimeService.update(user, id, in, ip);
                        flashSuccess(req, "Cập nhật suất chiếu thành công.");
                    } else {
                        showtimeService.create(user, in, ip);
                        flashSuccess(req, "Tạo suất chiếu thành công.");
                    }
                    redirect(req, resp, LIST + "?date=" + in.date + "&branchId=" + showtimeService.getBranchIdOfRoom(in.roomId));
                } catch (BusinessException e) {
                    // Lỗi nhập liệu: hiện lại form với dữ liệu khách vừa nhập
                    req.setAttribute("errors", e.getErrors());
                    Showtime existing = null;
                    if (id != null) {
                        try {
                            existing = showtimeService.get(user, id);
                        } catch (BusinessException ignored) {
                        }
                    }
                    prepareForm(req, user, existing);
                    render(req, resp, "showtime/form-hiep");
                }
                return;
            }
            case "/assign-room" -> {
                showtimeService.assignRoom(user, requireId(id), requireId(intParam(req, "roomId")), ip);
                flashSuccess(req, "Đã gán phòng mới cho suất chiếu (giá vé được tính lại theo phòng).");
            }
            case "/assign-movie" -> {
                showtimeService.assignMovie(user, requireId(id), requireId(intParam(req, "movieId")), ip);
                flashSuccess(req, "Đã gán phim mới cho suất chiếu (giờ kết thúc được tính lại).");
            }
            case "/cancel" -> {
                int n = showtimeService.cancel(user, requireId(id), ip);
                flashSuccess(req, "Đã hủy suất chiếu. " + n + " đơn vé được hủy (đơn đã thanh toán được hoàn tiền).");
            }
            case "/delete" -> {
                showtimeService.delete(user, requireId(id), ip);
                flashSuccess(req, "Xóa suất chiếu thành công.");
            }
            default -> {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        redirect(req, resp, redirectTo);
    }

    private int requireId(Integer id) throws BusinessException {
        if (id == null) throw new BusinessException("Dữ liệu gửi lên không hợp lệ.");
        return id;
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException, SQLException, BusinessException {
        int branchId = showtimeService.resolveBranchId(user, intParam(req, "branchId"));
        LocalDate date = dateParam(req, "date");
        if (date == null) date = LocalDate.now();
        Integer roomId = intParam(req, "roomId");
        req.setAttribute("branches", ShowtimeService.isAdmin(user) ? showtimeService.getAllBranches() : null);
        req.setAttribute("branch", showtimeService.getBranch(branchId));
        req.setAttribute("branchId", branchId);
        req.setAttribute("rooms", showtimeService.getRooms(branchId));
        req.setAttribute("movies", showtimeService.getSchedulableMovies(branchId));
        req.setAttribute("date", date.toString());
        req.setAttribute("prevDate", date.minusDays(1).toString());
        req.setAttribute("nextDate", date.plusDays(1).toString());
        req.setAttribute("dateLabel", DateTimeHelper.weekday(date) + ", " + DateTimeHelper.format(date)
                + (ShowtimePriceCalculator.isWeekend(date) ? " (cuối tuần)" : ""));
        req.setAttribute("roomId", roomId);
        req.setAttribute("showtimes", showtimeService.list(branchId, date, roomId));
        String qs = req.getQueryString();
        req.setAttribute("back", LIST + (qs == null ? "" : "?" + qs));
        req.setAttribute("nav", "showtimes");
        render(req, resp, "showtime/list-hiep");
    }

    private void prepareForm(HttpServletRequest req, User user, Showtime showtime) throws SQLException, BusinessException {
        int branchId = showtime != null ? showtime.getBranchId()
                : showtimeService.resolveBranchId(user, intParam(req, "branchId"));
        req.setAttribute("showtime", showtime);
        req.setAttribute("branch", showtimeService.getBranch(branchId));
        req.setAttribute("rooms", showtimeService.getRooms(branchId));
        req.setAttribute("movies", showtimeService.getSchedulableMovies(branchId));
        req.setAttribute("statuses", ShowtimeService.EDITABLE_STATUSES);
        req.setAttribute("standardPrice", ShowtimePriceCalculator.STANDARD_PRICE);
        req.setAttribute("weekendSurcharge", ShowtimePriceCalculator.WEEKEND_SURCHARGE);
        if (showtime == null && str(req, "date") != null && str(req, "startTime") == null) {
            req.setAttribute("defaultDate", str(req, "date"));
        }
        req.setAttribute("nav", "showtimes");
    }

    /** JSON cho form: {ready, ok, end, nextDay, buffer, price, weekend, conflicts:[...]} */
    private void checkOverlap(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException {
        Integer roomId = intParam(req, "roomId");
        Integer movieId = intParam(req, "movieId");
        LocalDate date = dateParam(req, "date");
        LocalTime start = timeParam(req, "startTime");
        Integer excludeId = intParam(req, "excludeId");
        try {
            StringBuilder sb = new StringBuilder("{");
            if (roomId != null && date != null) {
                BigDecimal price = showtimeService.previewPrice(roomId, date);
                sb.append("\"price\":").append(price.toBigInteger())
                  .append(",\"weekend\":").append(ShowtimePriceCalculator.isWeekend(date)).append(',');
            }
            if (roomId == null || movieId == null || date == null || start == null) {
                WebUtil.json(resp, sb.append("\"ready\":false}").toString());
                return;
            }
            OverlapResult r = showtimeService.checkOverlap(roomId, movieId, date, start, excludeId);
            sb.append("\"ready\":true,\"ok\":").append(r.isOk())
              .append(",\"end\":").append(WebUtil.js(r.getEndStr()))
              .append(",\"nextDay\":").append(r.isNextDay())
              .append(",\"buffer\":").append(r.getBufferMinutes())
              .append(",\"conflicts\":[");
            for (int i = 0; i < r.getConflicts().size(); i++) {
                Showtime s = r.getConflicts().get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"movie\":").append(WebUtil.js(s.getMovieTitle()))
                  .append(",\"start\":").append(WebUtil.js(s.getStartStr()))
                  .append(",\"end\":").append(WebUtil.js(s.getEndStr()))
                  .append(",\"date\":").append(WebUtil.js(s.getDateStr())).append('}');
            }
            WebUtil.json(resp, sb.append("]}").toString());
        } catch (BusinessException e) {
            WebUtil.json(resp, "{\"ready\":false,\"error\":" + WebUtil.js(e.getMessage()) + "}");
        }
    }

    private ShowtimeInput readInput(HttpServletRequest req) throws BusinessException {
        ShowtimeInput in = new ShowtimeInput();
        Integer movieId = intParam(req, "movieId");
        Integer roomId = intParam(req, "roomId");
        if (movieId == null) throw new BusinessException("Phim là bắt buộc.");
        if (roomId == null) throw new BusinessException("Phòng chiếu là bắt buộc.");
        in.movieId = movieId;
        in.roomId = roomId;
        in.date = dateParam(req, "date");
        in.start = timeParam(req, "startTime");
        if (in.date == null || in.start == null) {
            throw new BusinessException("Ngày chiếu và giờ bắt đầu là bắt buộc.");
        }
        in.status = str(req, "status");
        return in;
    }
}
