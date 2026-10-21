package controller.movie;

import controller.BaseServlet;
import model.User;
import service.movie.ShowtimeService;
import util.BusinessException;
import util.PermissionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** GET /manager/schedule?branchId=&from= : lịch chiếu tổng hợp 7 ngày (Phòng x Ngày) + tỷ lệ lấp đầy. */
@WebServlet("/manager/schedule")
public class ScheduleServlet extends BaseServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        User user = PermissionUtil.getCurrentUser(req);
        int branchId = showtimeService.resolveBranchId(user, intParam(req, "branchId"));
        LocalDate from = dateParam(req, "from");
        if (from == null) from = LocalDate.now().with(DayOfWeek.MONDAY);
        req.setAttribute("branches", ShowtimeService.isAdmin(user) ? showtimeService.getAllBranches() : null);
        req.setAttribute("branchId", branchId);
        req.setAttribute("schedule", showtimeService.buildSchedule(branchId, from, 7));
        req.setAttribute("nav", "schedule");
        render(req, resp, "showtime/schedule-hiep");
    }
}
