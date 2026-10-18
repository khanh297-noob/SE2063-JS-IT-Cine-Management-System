package util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import model.Branch;
import model.User;

/** Thuộc tính dùng chung cho thanh điều hướng (navbar + sidebar) của các màn hình quản lý chi nhánh. */
public class ManagerShell {

    /**
     * @param activeMenu         dashboard | branch-report | revenue | tickets
     * @param action             đường dẫn của trang hiện tại, dùng cho ô chọn chi nhánh trên navbar
     * @param scopingConfigured  DB đã có bảng phân quyền người dùng theo chi nhánh hay chưa
     */
    public static void apply(HttpServletRequest request, User user, boolean admin, String branchPill,
                             List<Branch> branches, Integer selectedBranchId, String activeMenu,
                             String action, boolean scopingConfigured) {
        request.setAttribute("navBadge", admin ? "ADMIN" : "MANAGER");
        request.setAttribute("roleLabel", admin ? "Administrator" : "Branch Manager");
        request.setAttribute("initials", initials(user.getFullName()));
        request.setAttribute("branchPill", branchPill);
        request.setAttribute("branches", branches);
        request.setAttribute("selectedBranchId", selectedBranchId);
        request.setAttribute("activeMenu", activeMenu);
        request.setAttribute("shellAction", action);
        if (!admin && !scopingConfigured) {
            request.setAttribute("scopeWarning",
                    "Chưa cấu hình phân quyền chi nhánh (bảng UserBranch): hiện đang hiển thị dữ liệu của tất cả chi nhánh.");
        }
    }

    public static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "U";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
