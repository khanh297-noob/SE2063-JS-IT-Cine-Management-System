package controller.branch;

import controller.BaseServlet;
import model.branch.BranchDetailDTO;
import service.branch.BranchService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import util.PermissionUtil;

/**
 * View Branch Details cho khách (bấm vào một rạp ở /branch). Trang riêng, không dùng layout trang Phim.
 * GET /branch/details?id=1   (không có id -> quay về danh sách rạp /branch)
 */
@WebServlet("/branch/details")
public class BranchDetailServlet extends BaseServlet {

    private final BranchService branchService = new BranchService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        Integer id = intParam(req, "id");
        if (id == null) {
            redirect(req, resp, "/branch");
            return;
        }
        BranchDetailDTO detail = branchService.getDetail(id);
        req.setAttribute("detail", detail);
        req.setAttribute("otherBranches", branchService.findOtherActive(id));
        req.setAttribute("isAdmin", PermissionUtil.isAdmin(req));
        render(req, resp, "customer/branch-hoang");
    }
}
