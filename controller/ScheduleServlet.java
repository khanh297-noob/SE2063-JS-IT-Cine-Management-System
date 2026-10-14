package cinemanagement.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;

import cinemanagement.dao.BranchDAO;
import cinemanagement.model.User;
import cinemanagement.service.ShowtimeService;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DateTimeHelper;
import cinemanagement.util.Web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Synthetic Showtime Schedule: lich tuan theo Phong x Ngay + thong ke lap day. */
@WebServlet("/manager/schedule")
public class ScheduleServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final ShowtimeService service = new ShowtimeService();
    private final BranchDAO branchDAO = new BranchDAO();

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User actor = Web.currentUser(req);
        int branchId;
        try {
            branchId = service.resolveBranchId(actor, Web.intParam(req, "branchId"));
        } catch (BusinessException e) {
            req.setAttribute("errorMessage", e.getMessage());
            Web.forward(req, resp, "error");
            return;
        }
        LocalDate from = DateTimeHelper.parseDate(req.getParameter("from"));
        if (from == null) {
            from = LocalDate.now().with(DayOfWeek.MONDAY);
        }
        req.setAttribute("branches", actor.isAdmin() ? branchDAO.findAll() : null);
        req.setAttribute("branchId", branchId);
        req.setAttribute("schedule", service.buildSchedule(branchId, from, 7));
        Web.forward(req, resp, "manager/schedule");
    }
}
