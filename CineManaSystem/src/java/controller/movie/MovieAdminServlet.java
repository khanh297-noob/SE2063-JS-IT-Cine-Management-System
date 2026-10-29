package controller.movie;

import controller.BaseServlet;
import service.movie.MovieStatusService;
import util.BusinessException;
import util.ForbiddenException;
import util.PermissionUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;

/**
 * Lớp cha cho các trang quản lý phim và thể loại (/manage/movie..., /manage/genres).
 * Chỉ Admin: kiểm tra ngay tại đây cho mọi GET / POST (AuthFilter dùng chung của nhóm chỉ chặn chung /manage/*
 * cho Admin và Quản lý chi nhánh), tài khoản khác -> /access-denied.
 * Trước mỗi lần mở trang, đối soát lại BR-MS-03 với dữ liệu hiện có trong database và ngày hôm nay,
 * để trạng thái phim luôn đúng BR-MS-01 (vd chi nhánh bị xoá thẳng trong DB, phim tới ngày khởi chiếu).
 * View nhận sẵn overdueMovies: phim "Coming Soon" đã quá ngày khởi chiếu nhưng chưa đủ điều kiện chiếu.
 */
public abstract class MovieAdminServlet extends BaseServlet {

    private final MovieStatusService ruleService = new MovieStatusService();

    @Override
    protected void checkAccess(HttpServletRequest req) {
        if (!PermissionUtil.isAdmin(req))
            throw new ForbiddenException("Chỉ Admin được quản lý phim và thể loại.");
    }

    @Override
    protected void beforeGet(HttpServletRequest req) throws SQLException, BusinessException {
        MovieStatusService.RuleCheckResult r = ruleService.enforceStatusRules();
        req.setAttribute("overdueMovies", r.getOverdue());
        String msg = r.toMessage();
        if (msg != null) flashError(req, msg);
    }
}
