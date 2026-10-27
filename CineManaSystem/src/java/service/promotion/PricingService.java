package service.promotion;

import dao.DBContext;
import dao.promotion.PricingDAO;
import service.common.AccessService;
import model.*;
import util.FormUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class PricingService {
    private final PricingDAO pricing = new PricingDAO();

    public List<Showtime> showtimes(User actor, int branch) throws SQLException {
        AccessService.configureBranch(actor, branch);
        try (Connection c = DBContext.getConnection()) { return pricing.listShowtimes(c, branch); }
    }
    public void ticketPrice(User actor, int id, BigDecimal price) throws SQLException {
        AccessService.active(actor);
        BigDecimal checked = FormUtil.amount(price, false);
        DBContext.transaction(c -> {
            Integer branch = pricing.showtimeBranchForUpdate(c, id);
            if (branch == null) throw new IllegalArgumentException("Không tìm thấy suất chiếu.");
            AccessService.configureBranch(actor, branch);
            if (pricing.setTicketPrice(c, id, checked) != 1) throw new SQLException("Không cập nhật được giá.");
            return null;
        });
    }
    public void fnbPrice(User actor, int item, Integer branch, BigDecimal price) throws SQLException {
        if (item <= 0) throw new IllegalArgumentException("Mã món không hợp lệ.");
        if (branch == null) AccessService.admin(actor); else AccessService.configureBranch(actor, branch);
        BigDecimal checked = price == null ? null : FormUtil.amount(price, false);
        if (branch == null && checked == null) throw new IllegalArgumentException("Giá chung không được trống.");
        DBContext.transaction(c -> {
            int count = branch == null ? pricing.setFnbBase(c, item, checked)
                    : pricing.setFnbOverride(c, branch, item, checked);
            if (count != 1) throw new IllegalArgumentException("Món chưa có hoặc chưa cấu hình ở chi nhánh.");
            return null;
        });
    }

    public void requireConfiguration(User actor) {
        AccessService.active(actor);
        if (!java.util.Set.of("Administrator", "Branch Management").contains(actor.getRole()))
            throw new SecurityException("Chỉ quản lý được cấu hình giá.");
    }
}
