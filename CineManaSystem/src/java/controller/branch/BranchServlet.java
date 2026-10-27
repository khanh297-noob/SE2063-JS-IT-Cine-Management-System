package controller.branch;

import model.branch.Branch;
import service.branch.BranchService;
import service.movie.MovieStatusService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Manage Branch information.
 * GET  /manage/branch                    danh sách (q, status); Quản lý chi nhánh chỉ thấy chi nhánh của mình
 * GET  /manage/branch?action=create      form thêm (chỉ Admin)
 * GET  /manage/branch?action=edit&id=1   form sửa (Admin, hoặc Quản lý của chi nhánh đó)
 * POST action=save | status              tạo mới và đổi trạng thái chỉ Admin
 */
@WebServlet("/manage/branch")
public class BranchServlet extends BranchScopedServlet {

    private final BranchService branchService = new BranchService();
    private final MovieStatusService movieStatusService = new MovieStatusService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "list")) {
            case "create" -> {
                requireAdmin(req);
                showForm(req, resp, new Branch());
            }
            case "edit" -> {
                int id = requiredId(req, "id");
                requireBranch(req, id);
                showForm(req, resp, branchService.get(id));
            }
            default -> {
                Integer managed = managedBranchId(req);
                if (managed != null) {
                    req.setAttribute("branches", List.of(branchService.get(managed)));
                } else {
                    String q = str(req, "q");
                    String status = str(req, "status");
                    req.setAttribute("branches", branchService.list(q, status));
                    req.setAttribute("q", q);
                    req.setAttribute("status", status);
                }
                render(req, resp, "branch/list-hoang");
            }
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        if ("status".equals(action(req, ""))) {
            requireAdmin(req);
            int id = requiredId(req, "id");
            String status = str(req, "status");
            branchService.changeStatus(id, status);
            flashSuccess(req, Branch.STATUS_ACTIVE.equals(status) ? "Đã mở lại chi nhánh." : "Đã ngưng hoạt động chi nhánh.");
            // BR-MS-03: chi nhánh đổi trạng thái có thể làm phim đủ / mất điều kiện đang chiếu
            String movieChanges = movieStatusService.enforceStatusRules().toMessage();
            if (movieChanges != null) flashError(req, movieChanges);
            redirect(req, resp, "/manage/branch");
            return;
        }

        Integer id = intParam(req, "id");
        if (id == null) requireAdmin(req);
        else requireBranch(req, id);

        Branch b = new Branch();
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
