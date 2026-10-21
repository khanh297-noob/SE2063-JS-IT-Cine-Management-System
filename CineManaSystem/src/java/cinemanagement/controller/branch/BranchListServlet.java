package cinemanagement.controller.branch;

import cinemanagement.controller.BaseServlet;
import cinemanagement.service.branch.BranchService;
import cinemanagement.util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import util.PermissionUtil;

/**
 * Danh sách rạp cho khách (nút "Rạp" trên header trang chủ). Trang riêng, không dùng layout trang Phim.
 * GET /branch   các chi nhánh đang hoạt động; bấm một rạp -> /branch/details?id=
 */
@WebServlet("/branch")
public class BranchListServlet extends BaseServlet {

    private final BranchService branchService = new BranchService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("branches", branchService.listActiveDetails());
        req.setAttribute("isAdmin", PermissionUtil.isAdmin(req));
        render(req, resp, "customer/branches-hoang");
    }
}
