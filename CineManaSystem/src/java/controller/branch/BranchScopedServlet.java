package controller.branch;

import controller.BaseServlet;
import model.User;
import util.ForbiddenException;
import util.PermissionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Lớp cha cho các trang quản lý chi nhánh / phòng chiếu / loại phòng (/manage/branch, /manage/rooms...).
 * AuthFilter đã chỉ cho Admin và Quản lý chi nhánh vào /manage/*; lớp này giới hạn thêm theo chi nhánh:
 * - Admin: toàn quyền mọi chi nhánh, được tạo chi nhánh, đổi trạng thái chi nhánh, sửa loại phòng.
 * - Quản lý chi nhánh: chỉ xem / sửa chi nhánh của mình (User.BranchID) và phòng chiếu của chi nhánh đó.
 * Vi phạm -> ForbiddenException -> BaseServlet chuyển sang /access-denied.
 *
 * Mọi view đều nhận sẵn: isAdmin (boolean), managedBranchId (null với Admin).
 */
public abstract class BranchScopedServlet extends BaseServlet {

    protected boolean isAdmin(HttpServletRequest req) {
        return PermissionUtil.isAdmin(req);
    }

    /**
     * Chi nhánh người đang đăng nhập được quản lý: null = mọi chi nhánh (Admin).
     * Quản lý chưa được gán chi nhánh thì không quản lý được gì.
     */
    protected Integer managedBranchId(HttpServletRequest req) {
        if (isAdmin(req)) return null;
        User user = PermissionUtil.getCurrentUser(req);
        Integer branchId = user == null ? null : user.getBranchID();
        if (branchId == null) throw new ForbiddenException("Tài khoản chưa được gán chi nhánh.");
        return branchId;
    }

    /** Chỉ Admin. */
    protected void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) throw new ForbiddenException("Chỉ Admin được thực hiện thao tác này.");
    }

    /** Admin, hoặc Quản lý của đúng chi nhánh branchId. */
    protected void requireBranch(HttpServletRequest req, int branchId) {
        Integer managed = managedBranchId(req);
        if (managed != null && managed != branchId)
            throw new ForbiddenException("Không có quyền với chi nhánh #" + branchId);
    }

    @Override
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.setAttribute("isAdmin", isAdmin(req));
        req.setAttribute("managedBranchId", managedBranchId(req));
        super.render(req, resp, view);
    }
}
