package service.f_b;

import dao.DBContext;
import dao.admin.BranchDAO;
import dao.f_b.BranchFBStockDAO;
import dao.f_b.F_BOrderDAO;
import service.common.AccessService;
import model.*;
import util.FormUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class F_BOrderService {
    private final F_BOrderDAO orders = new F_BOrderDAO();
    private final BranchFBStockDAO stocks = new BranchFBStockDAO();
    private final BranchDAO branches = new BranchDAO();

    public List<Booking> list(User actor, int branch) throws SQLException {
        AccessService.branch(actor, branch);
        try (Connection c = DBContext.getConnection()) { return orders.listForBranch(c, branch); }
    }
    public List<User> customers(User actor) throws SQLException {
        AccessService.fnb(actor);
        try (Connection c = DBContext.getConnection()) { return orders.customers(c); }
    }
    public Booking detail(User actor, int id) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            Booking b = orders.findBooking(c, id, false);
            requireAccess(actor, b);
            return b;
        }
    }
    public List<F_BOrder> lines(User actor, int id) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            requireAccess(actor, orders.findBooking(c, id, false));
            return orders.lines(c, id);
        }
    }

    public int create(User actor, int branch, int customer, Map<Integer, Integer> cart) throws SQLException {
        AccessService.active(actor);
        boolean online = "Customer".equals(actor.getRole());
        if (online) {
            if (actor.getUserID() != customer) throw new SecurityException("Không được đặt đơn cho người khác.");
        } else AccessService.branch(actor, branch);
        if (cart == null || cart.isEmpty() || cart.size() > 100)
            throw new IllegalArgumentException("Giỏ hàng phải có từ 1 đến 100 món.");
        Map<Integer, Integer> sorted = new TreeMap<>(cart);
        for (var e : sorted.entrySet())
            if (e.getKey() == null || e.getKey() <= 0 || e.getValue() == null || e.getValue() < 1)
                throw new IllegalArgumentException("MSG46: Số lượng phải là số nguyên dương.");
        return DBContext.transaction(c -> {
            if (!branches.existsActive(c, branch) || !orders.activeCustomer(c, customer))
                throw new IllegalArgumentException("Khách hoặc chi nhánh không hợp lệ.");
            Map<Integer, BigDecimal> prices = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (var e : sorted.entrySet()) {
                BranchFBStock menu = stocks.findSellableForUpdate(c, branch, e.getKey());
                if (menu == null) throw new IllegalArgumentException("MSG38: Món không bán tại chi nhánh.");
                if (menu.getStockQuantity() < e.getValue())
                    throw new IllegalArgumentException("MSG39: Không đủ tồn.");
                BigDecimal price = FormUtil.amount(menu.getEffectivePrice(), false);
                prices.put(e.getKey(), price);
                total = total.add(price.multiply(BigDecimal.valueOf(e.getValue())));
            }
            int id = orders.insertBooking(c, customer, branch, FormUtil.amount(total, false),
                    online ? null : actor.getUserID(), online ? "Online" : "Counter");
            for (var e : sorted.entrySet()) orders.insertLine(c, id, e.getKey(), e.getValue(), prices.get(e.getKey()));
            return id; // Pending chưa trừ kho.
        });
    }

    public void quantity(User actor, int booking, int item, int quantity) throws SQLException {
        if (item <= 0 || quantity < 1) throw new IllegalArgumentException("Số lượng phải >= 1.");
        DBContext.transaction(c -> {
            Booking b = orders.findBooking(c, booking, true);
            requireAccess(actor, b);
            requireEditable(c, b);
            if (b.getPromoId() != null) throw new IllegalArgumentException("Gỡ voucher trước khi sửa số lượng.");
            BranchFBStock menu = stocks.findSellableForUpdate(c, b.getBranchId(), item);
            if (menu == null || menu.getStockQuantity() < quantity)
                throw new IllegalArgumentException("Không đủ tồn hoặc món đã ngừng bán.");
            if (orders.updateQuantity(c, booking, item, quantity) != 1)
                throw new IllegalArgumentException("Không tìm thấy dòng Pending.");
            if (orders.updateTotal(c, booking, FormUtil.amount(orders.sumLines(c, booking), false)) != 1)
                throw new SQLException("Không cập nhật được tổng.");
            return null;
        });
    }

    public void cancel(User actor, int booking) throws SQLException {
        DBContext.transaction(c -> {
            Booking b = orders.findBooking(c, booking, true);
            requireAccess(actor, b);
            if ("Cancelled".equals(b.getStatus())) return null;
            requireEditable(c, b);
            List<F_BOrder> lines = orders.lines(c, booking);
            if (lines.isEmpty() || lines.stream().anyMatch(x -> !"Pending".equals(x.getOrderStatus())))
                throw new IllegalArgumentException("Dòng đơn không còn Pending.");
            if (orders.updateStatus(c, booking, "Pending", "Cancelled") != lines.size()
                    || orders.cancelBooking(c, booking) != 1)
                throw new SQLException("Không hủy được đơn.");
            return null; // Chưa trừ tồn nên không hoàn tồn.
        });
    }

    public void progress(User actor, int booking, String next) throws SQLException {
        AccessService.fnb(actor);
        DBContext.transaction(c -> {
            Booking b = orders.findBooking(c, booking, true);
            requireAccess(actor, b);
            if (!"Confirmed".equals(b.getStatus()) || !"Success".equals(b.getPaymentStatus()))
                throw new IllegalArgumentException("Chỉ chế biến sau khi thanh toán thành công.");
            List<F_BOrder> lines = orders.lines(c, booking);
            if (lines.isEmpty()) throw new IllegalArgumentException("Đơn không có món.");
            String current = lines.get(0).getOrderStatus();
            String expected = Map.of("Confirmed", "Preparing", "Preparing", "Ready", "Ready", "Delivered").get(current);
            if (next == null || !next.equals(expected)
                    || lines.stream().anyMatch(x -> !current.equals(x.getOrderStatus())))
                throw new IllegalArgumentException("Bước chuyển trạng thái không hợp lệ.");
            if (orders.updateStatus(c, booking, current, next) != lines.size())
                throw new SQLException("Không cập nhật đủ dòng.");
            return null;
        });
    }

    // Nam gọi bằng đúng Connection trong transaction thanh toán.
    public void confirmLinesInPayment(Connection c, int booking) throws SQLException {
        requireTransaction(c);
        Booking b = orders.findBooking(c, booking, true);
        if (b == null || b.getBranchId() == null || "Cancelled".equals(b.getStatus())
                || !"Success".equals(b.getPaymentStatus()))
            throw new IllegalArgumentException("Đơn chưa đủ điều kiện xác nhận.");
        List<F_BOrder> lines = orders.lines(c, booking);
        if (lines.isEmpty()) return;
        if (lines.stream().anyMatch(line -> line.getUnitPrice() == null)) {
            throw new IllegalArgumentException("Chưa chốt đơn giá F&B.");
        }
        if (lines.stream().allMatch(x -> Set.of("Confirmed", "Preparing", "Ready", "Delivered").contains(x.getOrderStatus()))) return;
        if (lines.stream().anyMatch(x -> !"Pending".equals(x.getOrderStatus())))
            throw new IllegalArgumentException("Trạng thái món không đồng nhất.");
        for (F_BOrder line : lines) {
            if (stocks.findSellableForUpdate(c, b.getBranchId(), line.getItemId()) == null
                    || stocks.takeStock(c, b.getBranchId(), line.getItemId(), line.getQuantity()) != 1)
                throw new IllegalArgumentException("MSG39: Hết tồn hoặc món ngừng bán.");
        }
        if (orders.updateStatus(c, booking, "Pending", "Confirmed") != lines.size())
            throw new SQLException("Không xác nhận đủ dòng.");
    }

    public void returnLinesInRefund(Connection c, int booking) throws SQLException {
        requireTransaction(c);
        Booking b = orders.findBooking(c, booking, true);
        if (b == null || b.getBranchId() == null || !"Refunded".equals(b.getPaymentStatus()))
            throw new IllegalArgumentException("Chưa có hoàn tiền hợp lệ.");
        for (F_BOrder line : orders.lines(c, booking)) {
            String status = line.getOrderStatus();
            if ("Cancelled".equals(status)) continue;
            if ("Delivered".equals(status)) throw new IllegalArgumentException("Món đã giao không được tự nhập kho.");
            if (Set.of("Confirmed", "Preparing", "Ready").contains(status)) {
                if (stocks.adjustStock(c, b.getBranchId(), line.getItemId(), line.getQuantity()) != 1)
                    throw new SQLException("Không hoàn được tồn.");
            } else if (!"Pending".equals(status)) throw new IllegalArgumentException("Trạng thái món không hợp lệ.");
            if (orders.updateOneStatus(c, booking, line.getItemId(), status, "Cancelled") != 1)
                throw new SQLException("Không hủy được dòng.");
        }
    }

    private void requireAccess(User actor, Booking b) {
        AccessService.active(actor);
        if (b == null || b.getBranchId() == null) throw new IllegalArgumentException("Không tìm thấy đơn có chi nhánh.");
        if ("Customer".equals(actor.getRole())) {
            if (b.getUserId() != actor.getUserID()) throw new SecurityException("Đơn không thuộc bạn.");
        } else AccessService.branch(actor, b.getBranchId());
    }
    private void requireEditable(Connection c, Booking b) throws SQLException {
        if (!"Pending".equals(b.getStatus()) || orders.paymentExists(c, b.getBookingId()))
            throw new IllegalArgumentException("Đơn đã vào thanh toán, không được sửa/hủy tại đây.");
        if (orders.hasTicket(c, b.getBookingId()))
            throw new IllegalArgumentException("Đơn có vé: xử lý qua Booking chung của Hiệp/Nam.");
        if (orders.lines(c, b.getBookingId()).stream()
                .anyMatch(line -> line.getUnitPrice() == null)) {
            throw new IllegalArgumentException(
                    "Đơn chưa lưu đơn giá lịch sử, không được tự tính lại giá hiện tại.");
        }
    }
    private void requireTransaction(Connection c) throws SQLException {
        if (c.getAutoCommit()) throw new IllegalStateException("Phải gọi trong transaction chung.");
    }
}
