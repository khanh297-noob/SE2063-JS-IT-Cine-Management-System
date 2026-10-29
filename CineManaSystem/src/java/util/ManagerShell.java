package util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import model.Branch;
import model.User;

/** Thuộc tính dùng chung cho thanh điều hướng (navbar + sidebar) của các màn hình quản lý chi nhánh. */
public class ManagerShell {

    /**
     * @param activeMenu         dashboard | branch-report | revenue | tickets | promo-hub | ticket-pricing | fnb-pricing | promo-validate
     * @param action             đường dẫn của trang hiện tại, dùng cho ô chọn chi nhánh trên navbar
     */
    public static void apply(HttpServletRequest request, User user, boolean admin, String branchPill,
                             List<Branch> branches, Integer selectedBranchId, String activeMenu,
                             String action) {
        boolean manager = PermissionUtil.isBranchManager(request);
        request.setAttribute("navBadge", admin ? "ADMIN" : (manager ? "MANAGER" : "POS"));
        request.setAttribute("roleLabel", admin ? "Administrator" : (manager ? "Branch Manager" : "POS Staff"));

        // Cờ quyền để sidebar chỉ hiện những mục người dùng được vào
        boolean viewReports = PermissionUtil.canViewSalesReport(request);
        request.setAttribute("canViewReports", viewReports);
        request.setAttribute("canManagePricing", PermissionUtil.canManagePricing(request));
        request.setAttribute("canValidatePromotion", PermissionUtil.canValidatePromotion(request));
        // Logo trên navbar dẫn về trang đầu của người dùng (thu ngân không xem được báo cáo)
        request.setAttribute("brandPath", viewReports ? "/report/branch-dashboard" : "/promotion-price-manage");
        request.setAttribute("initials", initials(user.getFullName()));
        request.setAttribute("branchPill", branchPill);
        request.setAttribute("branches", branches);
        request.setAttribute("selectedBranchId", selectedBranchId);
        request.setAttribute("activeMenu", activeMenu);
        request.setAttribute("shellAction", action);
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
