package util;

import java.util.ArrayList;
import java.util.List;
import model.User;

/**
 * Xác định người dùng được thao tác trên những chi nhánh nào (dùng chung cho báo cáo, giá, khuyến mãi).
 *
 * Quy ước danh sách chi nhánh:
 *   null           = được thao tác trên TẤT CẢ chi nhánh (Admin)
 *   danh sách rỗng = chưa được gán chi nhánh nào
 *   [1, 2, ...]    = chỉ các chi nhánh này (nhân viên chỉ có đúng 1: cột [User].BranchID)
 */
public class BranchScope {

    /** Các chi nhánh người dùng được phép thao tác (null = tất cả). */
    public static List<Integer> allowed(User user, boolean isAdmin) {
        if (isAdmin) {
            return null;
        }
        List<Integer> own = new ArrayList<>();
        if (user.getBranchID() != null) {
            own.add(user.getBranchID());
        }
        return own;
    }

    /**
     * Phạm vi chi nhánh cho một lần thao tác.
     *
     * @param requestedBranchId chi nhánh người dùng chọn (null nếu không chọn)
     * @return danh sách chi nhánh dùng để truy vấn (null = tất cả)
     * @throws SecurityException nếu chọn chi nhánh nằm ngoài phạm vi được phép
     */
    public static List<Integer> resolve(User user, boolean isAdmin, Integer requestedBranchId) {
        List<Integer> allowed = allowed(user, isAdmin);

        // Không chọn chi nhánh cụ thể: dùng toàn bộ phạm vi được phép
        if (requestedBranchId == null || requestedBranchId <= 0) {
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
