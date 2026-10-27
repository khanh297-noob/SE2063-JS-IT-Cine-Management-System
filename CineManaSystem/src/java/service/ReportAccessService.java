package service;

import dao.ReportDao;
import java.util.List;
import model.User;
import util.BranchScope;

/**
 * Xác định người dùng được xem báo cáo của những chi nhánh nào.
 *
 * Quy ước kết quả danh sách chi nhánh:
 *   null           = được xem TẤT CẢ chi nhánh
 *   danh sách rỗng = chưa được gán chi nhánh nào (không xem được gì)
 *   [1, 2, ...]    = chỉ được xem các chi nhánh này
 */
public class ReportAccessService {

    private final ReportDao reportDao;

    public ReportAccessService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /** Các chi nhánh người dùng được phép xem (null = tất cả). Xem BranchScope. */
    public List<Integer> allowedBranchIds(User user, boolean isAdmin) {
        return BranchScope.allowed(user, isAdmin);
    }

    /**
     * Xác định phạm vi chi nhánh cho một lần xem báo cáo.
     *
     * @throws SecurityException nếu chọn chi nhánh nằm ngoài phạm vi được phép
     */
    public List<Integer> resolveBranchScope(User user, boolean isAdmin, Integer requestedBranchId) {
        return BranchScope.resolve(user, isAdmin, requestedBranchId);
    }
}
