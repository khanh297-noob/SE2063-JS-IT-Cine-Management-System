package service;

import dao.ReportDao;
import java.util.ArrayList;
import java.util.List;
import model.User;

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

    /** Database đã có bảng UserBranch (phân quyền theo chi nhánh) hay chưa. */
    public boolean isBranchScopingConfigured() {
        return reportDao.hasUserBranchTable();
    }

    /**
     * Các chi nhánh người dùng được phép xem (null = tất cả).
     * Admin được xem tất cả. Quản lý chi nhánh thì lấy từ bảng UserBranch.
     * Nếu database chưa có bảng UserBranch thì chưa giới hạn được, coi như xem tất cả.
     */
    public List<Integer> allowedBranchIds(User user, boolean isAdmin) {
        if (isAdmin || !reportDao.hasUserBranchTable()) {
            return null;
        }
        return reportDao.getAuthorizedBranchIds(user.getUserID());
    }

    /**
     * Xác định phạm vi chi nhánh cho một lần xem báo cáo.
     *
     * @param requestedBranchId chi nhánh người dùng chọn trong bộ lọc (null nếu không chọn)
     * @return danh sách chi nhánh dùng để truy vấn (null = tất cả chi nhánh)
     * @throws SecurityException nếu chọn chi nhánh nằm ngoài phạm vi được phép
     */
    public List<Integer> resolveBranchScope(User user, boolean isAdmin, Integer requestedBranchId) {
        List<Integer> allowed = allowedBranchIds(user, isAdmin);

        // Không chọn chi nhánh cụ thể: dùng toàn bộ phạm vi được phép
        boolean hasFilter = requestedBranchId != null && requestedBranchId > 0;
        if (!hasFilter) {
            return allowed;
        }

        // Có chọn chi nhánh: phải nằm trong phạm vi được phép
        if (allowed != null && !allowed.contains(requestedBranchId)) {
            throw new SecurityException("Branch not authorized");
        }
        List<Integer> selected = new ArrayList<>();
        selected.add(requestedBranchId);
        return selected;
    }
}
