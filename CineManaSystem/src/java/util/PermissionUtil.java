package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.User;

public class PermissionUtil {
    private static final String SESSION_USER_KEY = "User";

    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object obj = session.getAttribute(SESSION_USER_KEY);
        if (obj instanceof User) return (User) obj;
        return null;
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    public static boolean hasRole(HttpServletRequest request, String role) {
        User user = getCurrentUser(request);
        if (user == null || role == null || user.getRole() == null) return false;
        return role.trim().equalsIgnoreCase(user.getRole().trim());
    }

    public static boolean hasAnyRole(HttpServletRequest request, String... roles) {
        User user = getCurrentUser(request);
        if (user == null || user.getRole() == null) return false;
        for (String role : roles) {
            if (role != null && role.trim().equalsIgnoreCase(user.getRole().trim())) {
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

    public static boolean isCustomer(HttpServletRequest request) {
        return hasRole(request, "Customer");
    }

    public static boolean isInternalUser(HttpServletRequest request) {
        return isAdmin(request) || isBranchManager(request) || isStaff(request);
    }

    public static boolean canViewSalesReport(HttpServletRequest request) {
        return isAdmin(request) || isBranchManager(request);
    }
}
