package service;
import model.User;

public final class AccessService {
    private AccessService() {}

    public static void active(User actor) {
        if (actor == null || !"Active".equals(actor.getStatus()))
            throw new SecurityException("Cần đăng nhập bằng tài khoản đang hoạt động.");
    }

    public static void admin(User actor) {
        active(actor);
        if (!"Administrator".equals(actor.getRole()))
            throw new SecurityException("Chỉ Admin được thao tác.");
    }

    public static int branch(User actor, int branchId) {
        active(actor);
        if (branchId <= 0) throw new IllegalArgumentException("Hãy chọn chi nhánh.");
        if ("Administrator".equals(actor.getRole())) return branchId;
        if (!"Branch Management".equals(actor.getRole())
                && !"Ticket Cashier".equals(actor.getRole()))
            throw new SecurityException("Không có quyền F&B.");
        if (actor.getBranchID() == null || actor.getBranchID().intValue() != branchId)
            throw new SecurityException("Không được thao tác chi nhánh khác.");
        return branchId;
    }

    public static void configureBranch(User actor, int branchId) {
        branch(actor, branchId);
        if (!"Administrator".equals(actor.getRole())
                && !"Branch Management".equals(actor.getRole()))
            throw new SecurityException("Chỉ quản lý được cấu hình chi nhánh.");
    }
    public static void fnb(User actor) {
        active(actor);
        if (!java.util.Set.of("Administrator","Branch Management","Ticket Cashier")
                .contains(actor.getRole()))
            throw new SecurityException("Không có quyền F&B.");
    }
    public static void promotion(User actor) {
        fnb(actor);
    }
}