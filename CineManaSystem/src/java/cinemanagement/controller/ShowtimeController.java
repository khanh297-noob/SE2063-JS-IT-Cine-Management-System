package cinemanagement.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import cinemanagement.model.OverlapResult;
import cinemanagement.model.Showtime;
import cinemanagement.model.User;
import cinemanagement.service.ShowtimeService;
import cinemanagement.service.ShowtimeService.ShowtimeInput;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DateTimeHelper;
import cinemanagement.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/manager/showtimes/*")
public class ShowtimeController extends HttpServlet {
    private ShowtimeService showtimeService;

    @Override
    public void init() throws ServletException {
        showtimeService = new ShowtimeService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        try {
            switch (path) {
                case "", "/" -> showList(request, response, user);
                case "/new" -> {
                    reloadFormContext(request, user, null);
                    request.getRequestDispatcher("/WEB-INF/views/manager/showtime-form-hiep.jsp").forward(request, response);
                }
                case "/edit" -> {
                    String idRaw = request.getParameter("id");
                    if (idRaw == null || idRaw.isEmpty()) {
                        session.setAttribute("errorMsg", "Vui lòng chọn một suất chiếu cụ thể để sửa.");
                        response.sendRedirect(request.getContextPath() + "/manager/showtimes");
                        return;
                    }
                    Showtime showtime = showtimeService.get(user, Integer.parseInt(idRaw));
                    reloadFormContext(request, user, showtime);
                    request.setAttribute("soldCount", showtime.getSoldCount());
                    request.getRequestDispatcher("/WEB-INF/views/manager/showtime-form-hiep.jsp").forward(request, response);
                }
                case "/check-overlap" -> checkOverlap(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã trên đường dẫn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/manager/showtimes");
        } catch (BusinessException e) {
            session.setAttribute("errorMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/manager");
        } catch (Exception e) {
            request.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải suất chiếu: " + e.getMessage());
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
        String idRaw = request.getParameter("id");
        String back = request.getParameter("back") == null ? "" : request.getParameter("back").trim();
        String redirect = back.startsWith("/manager/") ? back : "/manager/showtimes";

        try {
            int id = (idRaw == null || idRaw.isEmpty()) ? -1 : Integer.parseInt(idRaw);
            switch (path) {
                case "/save" -> {
                    try {
                        ShowtimeInput in = readInput(request);
                        if (id > 0) {
                            showtimeService.update(user, id, in, ip);
                            session.setAttribute("successMsg", "Cập nhật suất chiếu thành công. (MSG57)");
                        } else {
                            showtimeService.create(user, in, ip);
                            session.setAttribute("successMsg", "Tạo suất chiếu thành công. (MSG24)");
                        }
                        response.sendRedirect(request.getContextPath() + "/manager/showtimes?date=" + in.date
                                + "&branchId=" + showtimeService.getBranchIdOfRoom(in.roomId));
                    } catch (BusinessException e) {
                        request.setAttribute("errorMsg", e.getMessage());
                        Showtime existing = null;
                        if (id > 0) {
                            try {
                                existing = showtimeService.get(user, id);
                            } catch (BusinessException ignored) {
                            }
                        }
                        reloadFormContext(request, user, existing);
                        request.getRequestDispatcher("/WEB-INF/views/manager/showtime-form-hiep.jsp").forward(request, response);
                    }
                    return;
                }
                case "/assign-room" -> {
                    String roomIdRaw = request.getParameter("roomId");
                    int roomId = (roomIdRaw == null || roomIdRaw.isEmpty()) ? -1 : Integer.parseInt(roomIdRaw);
                    showtimeService.assignRoom(user, id, roomId, ip);
                    session.setAttribute("successMsg", "Đã gán phòng mới cho suất chiếu.");
                }
                case "/assign-movie" -> {
                    String movieIdRaw = request.getParameter("movieId");
                    int movieId = (movieIdRaw == null || movieIdRaw.isEmpty()) ? -1 : Integer.parseInt(movieIdRaw);
                    showtimeService.assignMovie(user, id, movieId, ip);
                    session.setAttribute("successMsg", "Đã gán phim mới cho suất chiếu (giờ kết thúc được tính lại).");
                }
                case "/cancel" -> {
                    int n = showtimeService.cancel(user, id, ip);
                    session.setAttribute("successMsg", "Đã hủy suất chiếu. " + n + " đơn vé được hủy và hoàn tiền.");
                }
                case "/delete" -> {
                    showtimeService.delete(user, id, ip);
                    session.setAttribute("successMsg", "Xóa suất chiếu thành công. (MSG05)");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            response.sendRedirect(request.getContextPath() + redirect);
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Dữ liệu gửi lên không hợp lệ.");
            response.sendRedirect(request.getContextPath() + redirect);
        } catch (BusinessException e) {
            session.setAttribute("errorMsg", e.getMessage());
            response.sendRedirect(request.getContextPath() + redirect);
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + redirect);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, User user) throws Exception {
        String branchIdRaw = request.getParameter("branchId");
        String roomIdRaw = request.getParameter("roomId");
        Integer requested = (branchIdRaw == null || branchIdRaw.isEmpty()) ? null : Integer.parseInt(branchIdRaw);
        int branchId = showtimeService.resolveBranchId(user, requested);
        LocalDate date = DateTimeHelper.parseDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }
        Integer roomId = (roomIdRaw == null || roomIdRaw.isEmpty()) ? null : Integer.parseInt(roomIdRaw);
        request.setAttribute("branches", user.isAdmin() ? showtimeService.getAllBranches() : null);
        request.setAttribute("branch", showtimeService.getBranch(branchId));
        request.setAttribute("branchId", branchId);
        request.setAttribute("rooms", showtimeService.getRooms(branchId));
        request.setAttribute("movies", showtimeService.getSchedulableMovies(branchId));
        request.setAttribute("date", date.toString());
        request.setAttribute("prevDate", date.minusDays(1).toString());
        request.setAttribute("nextDate", date.plusDays(1).toString());
        request.setAttribute("dateLabel", DateTimeHelper.weekday(date) + ", " + DateTimeHelper.format(date));
        request.setAttribute("roomId", roomId);
        request.setAttribute("showtimes", showtimeService.list(branchId, date, roomId));
        String qs = request.getQueryString();
        request.setAttribute("back", "/manager/showtimes" + (qs == null ? "" : "?" + qs));
        request.getRequestDispatcher("/WEB-INF/views/manager/showtime-list-hiep.jsp").forward(request, response);
    }

    private void reloadFormContext(HttpServletRequest request, User user, Showtime showtime) throws Exception {
        String branchIdRaw = request.getParameter("branchId");
        Integer requested = (branchIdRaw == null || branchIdRaw.isEmpty()) ? null : Integer.valueOf(branchIdRaw);
        int branchId = showtime != null ? showtime.getBranchId() : showtimeService.resolveBranchId(user, requested);
        request.setAttribute("showtime", showtime);
        request.setAttribute("branch", showtimeService.getBranch(branchId));
        request.setAttribute("rooms", showtimeService.getRooms(branchId));
        request.setAttribute("movies", showtimeService.getSchedulableMovies(branchId));
        request.setAttribute("statuses", ShowtimeService.EDITABLE_STATUSES);
        if (showtime == null && request.getParameter("date") != null && request.getParameter("startTime") == null) {
            request.setAttribute("defaultDate", request.getParameter("date"));
        }
    }

    private void checkOverlap(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer roomId = Web.intParam(request, "roomId");
        Integer movieId = Web.intParam(request, "movieId");
        LocalDate date = DateTimeHelper.parseDate(request.getParameter("date"));
        LocalTime start = DateTimeHelper.parseTime(request.getParameter("startTime"));
        Integer excludeId = Web.intParam(request, "excludeId");
        if (roomId == null || movieId == null || date == null || start == null) {
            Web.json(response, "{\"ready\":false}");
            return;
        }
        try {
            OverlapResult r = showtimeService.checkOverlap(roomId, movieId, date, start, excludeId);
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
            Web.json(response, sb.toString());
        } catch (BusinessException e) {
            Web.json(response, "{\"ready\":false,\"error\":" + Web.js(e.getMessage()) + "}");
        }
    }

    private ShowtimeInput readInput(HttpServletRequest request) throws BusinessException {
        ShowtimeInput in = new ShowtimeInput();
        Integer movieId = Web.intParam(request, "movieId");
        Integer roomId = Web.intParam(request, "roomId");
        if (movieId == null) {
            throw new BusinessException("Phim là bắt buộc. (MSG02)");
        }
        if (roomId == null) {
            throw new BusinessException("Phòng chiếu là bắt buộc. (MSG02)");
        }
        in.movieId = movieId;
        in.roomId = roomId;
        in.date = DateTimeHelper.parseDate(request.getParameter("date"));
        in.start = DateTimeHelper.parseTime(request.getParameter("startTime"));
        if (in.date == null || in.start == null) {
            throw new BusinessException("Ngày chiếu và giờ bắt đầu là bắt buộc. (MSG02)");
        }
        String priceRaw = request.getParameter("basePrice") == null ? "" : request.getParameter("basePrice").trim();
        try {
            in.price = new BigDecimal(priceRaw.replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new BusinessException("Giá vé không hợp lệ. (MSG07)");
        }
        in.status = request.getParameter("status");
        return in;
    }
}
