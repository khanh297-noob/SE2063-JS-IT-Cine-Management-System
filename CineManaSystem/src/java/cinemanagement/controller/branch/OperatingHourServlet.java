package cinemanagement.controller.branch;

import cinemanagement.controller.BaseServlet;
import cinemanagement.model.branch.BranchOperatingHour;
import cinemanagement.service.branch.BranchService;
import cinemanagement.service.branch.OperatingHourService;
import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage Operating Hours.
 * GET  /manage/branch/hours?branchId=1
 * POST branchId, closed_{1..7}, open_{1..7}, close_{1..7}
 */
@WebServlet("/manage/branch/hours")
public class OperatingHourServlet extends BaseServlet {

    private final BranchService branchService = new BranchService();
    private final OperatingHourService hourService = new OperatingHourService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int branchId = requiredId(req, "branchId");
        show(req, resp, branchId, hourService.getWeek(branchId));
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int branchId = requiredId(req, "branchId");
        List<BranchOperatingHour> week = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            BranchOperatingHour h = new BranchOperatingHour(branchId, d);
            h.setClosed(req.getParameter("closed_" + d) != null);
            h.setOpenTime(timeParam(req, "open_" + d));
            h.setCloseTime(timeParam(req, "close_" + d));
            week.add(h);
        }
        try {
            hourService.save(branchId, week);
            flashSuccess(req, "Đã lưu giờ mở cửa.");
            redirect(req, resp, "/manage/branch/hours?branchId=" + branchId);
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            show(req, resp, branchId, week);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, int branchId, List<BranchOperatingHour> week)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("branch", branchService.get(branchId));
        req.setAttribute("week", week);
        render(req, resp, "branch/operating-hours-hoang");
    }
}
