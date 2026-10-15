package service;

import dao.ReportDao;
import java.util.ArrayList;
import java.util.List;
import model.User;

/** Xác định phạm vi chi nhánh mà người dùng được phép xem trong báo cáo. */
public class ReportAccessService {

    private final ReportDao reportDao;

    public ReportAccessService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /** DB đã có bảng UserBranch để giới hạn người dùng theo chi nhánh hay chưa. */
    public boolean isBranchScopingConfigured() {
        return reportDao.hasUserBranchTable();
    }

    /**
     * Các chi nhánh người dùng được phép xem; null nghĩa là tất cả.
     * Admin được xem tất cả. Với người dùng khác, phạm vi lấy từ bảng UserBranch.
     * Nếu DB chưa có bảng UserBranch thì chưa thể giới hạn theo chi nhánh (xem ghi chú bàn giao).
     */
    public List<Integer> allowedBranchIds(User user, boolean isAdmin) {
        if (isAdmin || !reportDao.hasUserBranchTable()) {
            return null;
        }
        return reportDao.getAuthorizedBranchIds(user.getUserID());
    }

    /**
     * @param requestedBranchId chi nhánh được chọn trong bộ lọc (null nếu không chọn)
     * @return null nếu xem tất cả chi nhánh; ngược lại là danh sách chi nhánh áp dụng cho truy vấn
     * @throws SecurityException nếu yêu cầu chi nhánh nằm ngoài phạm vi được phép
     */
    public List<Integer> resolveBranchScope(User user, boolean isAdmin, Integer requestedBranchId) {
        List<Integer> allowed = allowedBranchIds(user, isAdmin);
        boolean hasFilter = requestedBranchId != null && requestedBranchId > 0;

        if (!hasFilter) {
            return allowed;
        }
        if (allowed != null && !allowed.contains(requestedBranchId)) {
            throw new SecurityException("Branch not authorized");
        }
        List<Integer> one = new ArrayList<>();
        one.add(requestedBranchId);
        return one;
    }
}
