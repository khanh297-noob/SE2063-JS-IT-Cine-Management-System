package cinemanagement.controller.branch;

import cinemanagement.controller.home.CustomerBaseServlet;
import cinemanagement.model.BranchDetailDTO;
import cinemanagement.util.BusinessException;
import cinemanagement.model.Branch;
import cinemanagement.service.BranchService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * View Branch Details, thuộc Giao diện khách hàng (header khách hàng, menu "Chi nhánh").
 * GET /branch?id=1   (không có id -> chuyển tới chi nhánh đầu tiên đang hoạt động)
 */
@WebServlet("/branch")
public class BranchDetailServlet extends CustomerBaseServlet {

    private final BranchService branchService = new BranchService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        Integer id = intParam(req, "id");
        if (id == null) {
            List<Branch> active = branchService.findActive();
            if (active.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Chưa có chi nhánh nào hoạt động.");
            } else {
                redirect(req, resp, "/branch?id=" + active.get(0).getBranchId());
            }
            return;
        }
        BranchDetailDTO detail = branchService.getDetail(id);
        prepareLayout(req);
        req.setAttribute("detail", detail);
        req.setAttribute("otherBranches", branchService.findOtherActive(id));
        req.setAttribute("customerNav", "branch");
        render(req, resp, "customer/branch-hoang");
    }
}
