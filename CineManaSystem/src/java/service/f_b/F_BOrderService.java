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
        //kiem tra gio hang
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

    public void returnLinesInRefund(Connection c, int bookingId)
            throws SQLException {
        requireTransaction(c);

        Booking b = orders.findBooking(c, bookingId, true);

        if (b == null || b.getBranchId() == null
                || !"Cancelled".equals(b.getStatus())
                || !"Refunded".equals(b.getPaymentStatus())) {
            throw new IllegalArgumentException(
                    "Chưa có hủy/hoàn tiền hợp lệ.");
        }

        List<F_BOrder> lines = orders.lines(c, bookingId);

        // Kiểm tra tất cả trước khi thay đổi kho.
        for (F_BOrder line : lines) {
            String status = line.getOrderStatus();

            if (!"Pending".equals(status)
                    && !"Confirmed".equals(status)
                    && !"Cancelled".equals(status)) {
                throw new IllegalArgumentException(
                        "Món đã chế biến hoặc giao; không được tự nhập lại kho.");
            }
        }

        for (F_BOrder line : lines) {
            String status = line.getOrderStatus();

            // Gửi lại yêu cầu không cộng tồn lần nữa.
            if ("Cancelled".equals(status)) {
                continue;
            }

            // Chỉ Confirmed đã trừ kho nên mới được cộng lại.
            if ("Confirmed".equals(status)
                    && stocks.adjustStock(
                    c,
                    b.getBranchId(),
                    line.getItemId(),
                    line.getQuantity()) != 1) {
                throw new SQLException("Không hoàn được tồn kho.");
            }

            if (orders.updateOneStatus(
                    c,
                    bookingId,
                    line.getItemId(),
                    status,
                    "Cancelled") != 1) {
                throw new SQLException("Dòng món đã thay đổi.");
            }
        }
    }

    // Dùng để quyết định có hiển thị form thanh toán hay không.
// Khi POST, payCash vẫn kiểm tra lại toàn bộ điều kiện.
    public boolean canPayCash(User actor, int bookingId) throws SQLException {
        AccessService.fnb(actor);

        try (Connection c = DBContext.getConnection()) {
            Booking b = orders.findBooking(c, bookingId, false);
            requireAccess(actor, b);

            if (!"Pending".equals(b.getStatus())
                    || orders.paymentExists(c, bookingId)
                    || orders.hasTicket(c, bookingId)
                    || !b.getBookingDate().toLocalDateTime().plusMinutes(10)
                    .isAfter(java.time.LocalDateTime.now())) {
                return false;
            }

            List<F_BOrder> lines = orders.lines(c, bookingId);

            return !lines.isEmpty()
                    && lines.stream().allMatch(line ->
                    "Pending".equals(line.getOrderStatus())
                            && line.getUnitPrice() != null);
        }
    }

    // Chỉ xử lý đơn F&B độc lập, không chứa vé.
    public BigDecimal payCash(
            User actor,
            int bookingId,
            BigDecimal cashReceived,
            String ip) throws SQLException {

        AccessService.fnb(actor);

        BigDecimal received = FormUtil.amount(cashReceived, true);

        return DBContext.transaction(c -> {
            // Khóa Booking để hai yêu cầu không cùng thu tiền một đơn.
            Booking b = orders.findBooking(c, bookingId, true);

            requireAccess(actor, b);

            // Hàm có sẵn:
            // Pending, chưa có Payment, không chứa vé, đã lưu UnitPrice.
            requireEditable(c, b);

            // Giữ thời hạn 10 phút theo luồng hiện tại.
            if (!b.getBookingDate().toLocalDateTime().plusMinutes(10)
                    .isAfter(java.time.LocalDateTime.now())) {
                throw new IllegalArgumentException(
                        "Đơn đã quá 10 phút. Hãy tạo đơn mới.");
            }

            List<F_BOrder> lines = orders.lines(c, bookingId);

            if (lines.isEmpty()
                    || lines.stream().anyMatch(line ->
                    !"Pending".equals(line.getOrderStatus()))) {
                throw new IllegalArgumentException(
                        "Đơn không có món hoặc dòng món không còn Pending.");
            }

            if (received.compareTo(b.getTotalAmount()) < 0) {
                throw new IllegalArgumentException(
                        "Tiền khách đưa chưa đủ. Cần thu "
                                + b.getTotalAmount().toPlainString() + " đ.");
            }


            if ("Ticket Cashier".equals(actor.getRole())) {
                model.pos.WorkShift shift =
                        new service.pos.ShiftService().getActiveShift(actor);

                if (shift == null || shift.getBranchId() != b.getBranchId()) {
                    throw new IllegalArgumentException(
                            "Hãy vào ca tại đúng chi nhánh trước khi thu tiền.");
                }
            }

            // Kiểm tra lại tổng tiền và voucher trước khi thu.
            new service.promotion.PromotionService()
                    .revalidateInPayment(c, bookingId);

            //Ghi payment thành công
            if (orders.insertCashPayment(
                    c, bookingId, b.getTotalAmount()) != 1) {
                throw new SQLException("Không ghi nhận được thanh toán.");
            }

            // Hàm đã có trong file:
            // kiểm tra kho, trừ kho, đổi dòng món Pending -> Confirmed.
            confirmLinesInPayment(c, bookingId);

            if (orders.confirmCashBooking(c, bookingId, actor.getUserID()) != 1) {
                throw new SQLException("Không xác nhận được Booking.");
            }

            BigDecimal change = received.subtract(b.getTotalAmount());

            // Liên quan Nam: giữ định dạng log thu tiền mặt của POS.
            new dao.admin.AuditLogDAO().insert(
                    c,
                    actor.getUserID(),
                    "COUNTER_SALE|booking=" + bookingId
                            + "|amount=" + b.getTotalAmount().toPlainString()
                            + "|method=Cash"
                            + "|received=" + received.toPlainString()
                            + "|change=" + change.toPlainString(),
                    ip
            );

            return change;
        });
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
            throw new IllegalArgumentException("Đơn có vé: xử lý qua Booking.");
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
