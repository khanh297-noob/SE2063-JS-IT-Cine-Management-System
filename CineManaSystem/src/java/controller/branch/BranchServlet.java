package controller.branch;

import controller.BaseServlet;
import model.branch.Branch;
import service.branch.BranchService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Manage Branch information.
 * GET  /manage/branch                    danh sách (q, status)
 * GET  /manage/branch?action=create      form thêm
 * GET  /manage/branch?action=edit&id=1   form sửa
 * POST action=save | status
 */
@WebServlet("/manage/branch")
public class BranchServlet extends BaseServlet {

    private final BranchService branchService = new BranchService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "list")) {
            case "create" -> showForm(req, resp, new Branch());
            case "edit" -> showForm(req, resp, branchService.get(requiredId(req, "id")));
            default -> {
                String q = str(req, "q");
                String status = str(req, "status");
                req.setAttribute("branches", branchService.list(q, status));
                req.setAttribute("q", q);
                req.setAttribute("status", status);
                render(req, resp, "branch/list-hoang");
            }
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        if ("status".equals(action(req, ""))) {
            int id = requiredId(req, "id");
            String status = str(req, "status");
            branchService.changeStatus(id, status);
            flashSuccess(req, Branch.STATUS_ACTIVE.equals(status) ? "Đã mở lại chi nhánh." : "Đã ngưng hoạt động chi nhánh.");
            redirect(req, resp, "/manage/branch");
            return;
        }

        Branch b = new Branch();
        Integer id = intParam(req, "id");
        b.setBranchId(id == null ? 0 : id);
        b.setName(str(req, "name"));
        b.setAddress(str(req, "address"));
        b.setCity(str(req, "city"));
        b.setHotline(str(req, "hotline"));
        b.setEmail(str(req, "email"));
        try {
            branchService.save(b);
            flashSuccess(req, (id == null ? "Đã thêm " : "Đã cập nhật ") + b.getName() + ".");
            redirect(req, resp, "/manage/branch");
        } catch (BusinessException e) {
            req.setAttribute("errors", e.getErrors());
            showForm(req, resp, b);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Branch b)
            throws ServletException, IOException {
        req.setAttribute("branch", b);
        render(req, resp, "branch/form-hoang");
    }
}
