package dao.f_b;

import model.Booking;
import model.F_BOrder;
import model.User;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class F_BOrderDAO {
    private static final String SELECT_BOOKING = """
            SELECT b.BookingID, b.UserID, b.BranchID, b.PromoID, b.TotalAmount,
                   b.DiscountApplied, b.Status, b.Channel, b.BookingDate,
                   u.FullName AS CustomerName, p.GatewayStatus AS PaymentStatus
            FROM dbo.Booking b
            JOIN dbo.[User] u ON u.UserID = b.UserID
            LEFT JOIN dbo.Payment p ON p.BookingID = b.BookingID
            """;

    public List<Booking> listForBranch(Connection c, int branch) throws SQLException {
        List<Booking> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_BOOKING + """
                WHERE b.BranchID = ?
                  AND EXISTS(SELECT 1 FROM dbo.Booking_F_B f WHERE f.BookingID = b.BookingID)
                ORDER BY b.BookingDate DESC, b.BookingID DESC
                """)) {
            ps.setInt(1, branch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapBooking(rs));
            }
        }
        return result;
    }

    public Booking findBooking(Connection c, int id, boolean lock) throws SQLException {
        String sql = SELECT_BOOKING;
        if (lock) sql = sql.replace("dbo.Booking b", "dbo.Booking b WITH (UPDLOCK, HOLDLOCK)");
        try (PreparedStatement ps = c.prepareStatement(sql + " WHERE b.BookingID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapBooking(rs) : null; }
        }
    }

    public List<F_BOrder> lines(Connection c, int booking) throws SQLException {
        List<F_BOrder> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT x.BookingID, x.ItemID, x.Quantity, x.UnitPrice,
                       x.OrderStatus, x.OrderDate, f.ItemName
                FROM dbo.Booking_F_B x JOIN dbo.F_B f ON f.ItemID = x.ItemID
                WHERE x.BookingID = ? ORDER BY x.ItemID
                """)) {
            ps.setInt(1, booking);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new F_BOrder(rs.getInt("BookingID"), rs.getInt("ItemID"),
                            rs.getInt("Quantity"), rs.getString("ItemName"), rs.getBigDecimal("UnitPrice"),
                            rs.getString("OrderStatus"), rs.getTimestamp("OrderDate")));
                }
            }
        }
        return result;
    }

    public List<User> customers(Connection c) throws SQLException {
        List<User> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT UserID, FullName, Email FROM dbo.[User]
                WHERE Role = 'Customer' AND Status = 'Active' ORDER BY FullName
                """); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setUserID(rs.getInt("UserID"));
                u.setFullName(rs.getString("FullName"));
                u.setEmail(rs.getString("Email"));
                result.add(u);
            }
        }
        return result;
    }

    public boolean activeCustomer(Connection c, int id) throws SQLException {
        return exists(c, "SELECT 1 FROM dbo.[User] WHERE UserID = ? AND Role = 'Customer' AND Status = 'Active'", id);
    }
    public boolean hasTicket(Connection c, int id) throws SQLException {
        return exists(c, "SELECT 1 FROM dbo.Ticket WHERE BookingID = ?", id);
    }
    public boolean paymentExists(Connection c, int id) throws SQLException {
        return exists(c, "SELECT 1 FROM dbo.Payment WHERE BookingID = ?", id);
    }

    public int insertBooking(Connection c, int customer, int branch, BigDecimal total,
                             Integer staff, String channel) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO dbo.Booking(UserID, BranchID, TotalAmount, Status, StaffID, Channel)
                VALUES(?, ?, ?, 'Pending', ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customer); ps.setInt(2, branch); ps.setBigDecimal(3, total);
            if (staff == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, staff);
            ps.setString(5, channel); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Không lấy được BookingID.");
                return rs.getInt(1);
            }
        }
    }

    public void insertLine(Connection c, int booking, int item, int quantity, BigDecimal price)
            throws SQLException {
        update(c, """
                INSERT INTO dbo.Booking_F_B(BookingID, ItemID, Quantity, UnitPrice, OrderStatus)
                VALUES(?, ?, ?, ?, 'Pending')
                """, booking, item, quantity, price);
    }
    public int updateQuantity(Connection c, int booking, int item, int quantity) throws SQLException {
        return update(c, "UPDATE dbo.Booking_F_B SET Quantity = ? WHERE BookingID = ? AND ItemID = ? AND OrderStatus = 'Pending'",
                quantity, booking, item);
    }
    public BigDecimal sumLines(Connection c, int booking) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(Quantity * UnitPrice), 0)
                FROM dbo.Booking_F_B WHERE BookingID = ? AND OrderStatus <> 'Cancelled'
                """)) {
            ps.setInt(1, booking);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }
    public int updateTotal(Connection c, int booking, BigDecimal total) throws SQLException {
        return update(c, "UPDATE dbo.Booking SET TotalAmount = ? WHERE BookingID = ? AND Status = 'Pending'", total, booking);
    }
    public int updateStatus(Connection c, int booking, String from, String to) throws SQLException {
        return update(c, "UPDATE dbo.Booking_F_B SET OrderStatus = ? WHERE BookingID = ? AND OrderStatus = ?", to, booking, from);
    }
    public int updateOneStatus(Connection c, int booking, int item, String from, String to) throws SQLException {
        return update(c, "UPDATE dbo.Booking_F_B SET OrderStatus = ? WHERE BookingID = ? AND ItemID = ? AND OrderStatus = ?",
                to, booking, item, from);
    }
    public int cancelBooking(Connection c, int booking) throws SQLException {
        return update(c, "UPDATE dbo.Booking SET Status = 'Cancelled', CancelledAt = SYSDATETIME() WHERE BookingID = ? AND Status = 'Pending'", booking);
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("BookingID")); b.setUserId(rs.getInt("UserID"));
        b.setBranchId((Integer) rs.getObject("BranchID")); b.setPromoId((Integer) rs.getObject("PromoID"));
        b.setTotalAmount(rs.getBigDecimal("TotalAmount")); b.setDiscountApplied(rs.getBigDecimal("DiscountApplied"));
        b.setStatus(rs.getString("Status")); b.setChannel(rs.getString("Channel"));
        b.setCustomerName(rs.getString("CustomerName")); b.setPaymentStatus(rs.getString("PaymentStatus"));
        b.setBookingDate(rs.getTimestamp("BookingDate"));
        return b;
    }
    private int update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            return ps.executeUpdate();
        }
    }

    private boolean exists(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
