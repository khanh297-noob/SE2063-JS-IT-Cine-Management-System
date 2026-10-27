package service.f_b;

import dao.DBContext;
import dao.admin.AuditLogDAO;
import dao.admin.BranchDAO;
import dao.f_b.BranchFBStockDAO;
import dao.f_b.F_BItemDAO;
import service.common.AccessService;
import model.*;
import util.FormUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Set;

public class F_BStockService {
    private final BranchDAO branches = new BranchDAO();
    private final BranchFBStockDAO stocks = new BranchFBStockDAO();
    private final F_BItemDAO items = new F_BItemDAO();
    private final AuditLogDAO logs = new AuditLogDAO();

    public List<Branch> allowedBranches(User actor) throws SQLException {
        AccessService.fnb(actor);
        if ("Administrator".equals(actor.getRole())) return branches.getActiveBranches(null);
        if (actor.getBranchID() == null) throw new SecurityException("Tài khoản chưa được gán chi nhánh.");
        return branches.getActiveBranches(actor.getBranchID());
    }

    public List<BranchFBStock> list(User actor, int branch) throws SQLException {
        AccessService.branch(actor, branch);
        try (Connection c = DBContext.getConnection()) {
            requireActiveBranch(c, branch);
            return stocks.findForBranch(c, branch);
        }
    }

    public List<BranchFBStock> saleMenu(User actor, int branch) throws SQLException {
        AccessService.active(actor);
        if (!"Customer".equals(actor.getRole())) AccessService.branch(actor, branch);
        try (Connection c = DBContext.getConnection()) {
            requireActiveBranch(c, branch);
            return stocks.findForBranch(c, branch).stream()
                    .filter(x -> x.isItemActive() && x.isAvailable() && x.getStockQuantity() > 0)
                    .toList();
        }
    }

    public List<F_BItem> catalog(User actor, int branch) throws SQLException {
        AccessService.configureBranch(actor, branch);
        try (Connection c = DBContext.getConnection()) {
            requireActiveBranch(c, branch);
            return items.findAll(c);
        }
    }

    public void configure(User actor, int branch, int item, BigDecimal price, String status)
            throws SQLException {
        AccessService.configureBranch(actor, branch);
        if (item <= 0 || status == null || !Set.of("Available", "SoldOut", "Inactive").contains(status))
            throw new IllegalArgumentException("Món hoặc trạng thái không hợp lệ.");
        BigDecimal checked = price == null ? null : FormUtil.amount(price, false);
        DBContext.transaction(c -> {
            requireActiveBranch(c, branch);
            F_BItem catalogItem = items.findByIdForUpdate(c, item);
            if (catalogItem == null) throw new IllegalArgumentException("Không tìm thấy món.");
            if ("Available".equals(status) && !catalogItem.isActive()) {
                throw new IllegalArgumentException("Admin đã ngừng bán món toàn hệ thống.");
            }
            stocks.configure(c, branch, item, checked, status);
            return null;
        });
    }

    public void adjust(User actor, int branch, int item, int delta, String ip) throws SQLException {
        AccessService.configureBranch(actor, branch);
        if (item <= 0 || delta == 0) throw new IllegalArgumentException("Số điều chỉnh phải khác 0.");
        DBContext.transaction(c -> {
            requireActiveBranch(c, branch);
            if (stocks.adjustStock(c, branch, item, delta) != 1)
                throw new IllegalArgumentException("Món chưa cấu hình, tồn âm hoặc vượt giới hạn.");
            logs.insert(c, actor.getUserID(),
                    "STOCK_ADJUST|branch=" + branch + "|item=" + item + "|delta=" + delta, ip);
            return null;
        });
    }

    private void requireActiveBranch(Connection c, int branch) throws SQLException {
        if (branch <= 0 || !branches.existsActive(c, branch))
            throw new IllegalArgumentException("Chi nhánh không hoạt động.");
    }

    public boolean canConfigure(User actor) {
        AccessService.fnb(actor);
        return "Administrator".equals(actor.getRole())
                || "Branch Management".equals(actor.getRole());
    }
}
