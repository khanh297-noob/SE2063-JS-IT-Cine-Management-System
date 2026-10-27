package util;

import jakarta.servlet.http.HttpServletRequest;
import model.User;

/**
 * Kiểm tra người dùng hiện tại đã đăng nhập chưa và có vai trò gì.
 * So sánh vai trò không phân biệt hoa thường.
 */
public class PermissionUtil {

    /** Lấy user đang đăng nhập (null nếu chưa đăng nhập). */
    public static User getCurrentUser(HttpServletRequest request) {
        return SessionUtil.getLoggedInUser(request);
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    /** Người dùng hiện tại có đúng vai trò này không. */
    public static boolean hasRole(HttpServletRequest request, String role) {
        return hasAnyRole(request, role);
    }

    /** Người dùng hiện tại có một trong các vai trò này không. */
    public static boolean hasAnyRole(HttpServletRequest request, String... roles) {
        User user = getCurrentUser(request);
        if (user == null || user.getRole() == null) {
            return false;
        }
        String userRole = user.getRole().trim();
        for (String role : roles) {
            if (role != null && role.trim().equalsIgnoreCase(userRole)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return hasAnyRole(request, "Admin", "Administrator");
    }

    public static boolean isBranchManager(HttpServletRequest request) {
        return hasAnyRole(request, "Branch Management", "BranchManager", "Manager");
    }

    public static boolean isStaff(HttpServletRequest request) {
        return hasAnyRole(request, "Staff", "Ticket Cashier", "Ticket Checker", "Cashier");
    }

    /** Thu ngân bán vé tại quầy (POS Staff). */
    public static boolean isCashier(HttpServletRequest request) {
        return hasAnyRole(request, "Ticket Cashier", "Cashier");
    }

    public static boolean isCustomer(HttpServletRequest request) {
        return hasRole(request, "Customer");
    }

    /** Nhân sự nội bộ: Admin, Quản lý chi nhánh hoặc Nhân viên. */
    public static boolean isInternalUser(HttpServletRequest request) {
        return isAdmin(request) || isBranchManager(request) || isStaff(request);
    }

    /** Chỉ Admin và Quản lý chi nhánh được xem báo cáo. */
    public static boolean canViewSalesReport(HttpServletRequest request) {
        return isAdmin(request) || isBranchManager(request);
    }

    /** Quản lý giá vé và giá F&B: Admin và Quản lý chi nhánh. */
    public static boolean canManagePricing(HttpServletRequest request) {
        return isAdmin(request) || isBranchManager(request);
    }

    /** Kiểm tra mã khuyến mãi cho đơn hàng: Thu ngân (POS Staff) và Quản lý chi nhánh. */
    public static boolean canValidatePromotion(HttpServletRequest request) {
        return isCashier(request) || isBranchManager(request);
    }

    /** Vào trang tổng hợp Promotion & Pricing: người có ít nhất một chức năng trong đó. */
    public static boolean canOpenPromotionPricing(HttpServletRequest request) {
        return canManagePricing(request) || canValidatePromotion(request);
    }
}
