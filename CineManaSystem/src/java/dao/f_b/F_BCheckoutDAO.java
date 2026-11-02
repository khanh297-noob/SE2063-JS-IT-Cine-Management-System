package dao.f_b;

import java.math.BigDecimal;
import java.sql.*;
import model.payment.Payment;

/** Chỉ SQL; mọi phương thức dùng Connection do Service truyền. */
public class F_BCheckoutDAO {
    public Payment payment(Connection c, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT PaymentID, BookingID, Amount, PaymentMethod,
                       GatewayStatus, TransactionCode
                FROM dbo.Payment WITH (UPDLOCK, HOLDLOCK)
                WHERE BookingID = ?
                """)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Payment p = new Payment();
                p.setPaymentId(rs.getInt("PaymentID"));
                p.setBookingId(rs.getInt("BookingID"));
                p.setAmount(rs.getBigDecimal("Amount"));
                p.setPaymentMethod(rs.getString("PaymentMethod"));
                p.setGatewayStatus(rs.getString("GatewayStatus"));
                p.setTransactionCode(rs.getString("TransactionCode"));
                return p;
            }
        }
    }

    public void insertPayment(Connection c, int booking, BigDecimal amount,
                              String method, String reference, String status)
            throws SQLException {
        execute(c, """
                INSERT INTO dbo.Payment
                    (BookingID, Amount, PaymentMethod, PaymentTime,
                     TransactionCode, GatewayStatus)
                VALUES (?, ?, ?, SYSDATETIME(), ?, ?)
                """, booking, amount, method, reference, status);
    }

    public int bindReference(Connection c, int booking, String reference) throws SQLException {
        return execute(c, """
                UPDATE dbo.Payment SET TransactionCode = ?
                WHERE BookingID = ? AND GatewayStatus = 'Pending'
                """, reference, booking);
    }

    public int paymentSuccess(Connection c, int booking, String reference) throws SQLException {
        return execute(c, """
                UPDATE dbo.Payment
                SET GatewayStatus = 'Success', PaymentTime = SYSDATETIME(),
                    TransactionCode = COALESCE(?, TransactionCode)
                WHERE BookingID = ? AND GatewayStatus = 'Pending'
                """, reference, booking);
    }

    public int confirmBooking(Connection c, int booking) throws SQLException {
        return execute(c, """
                UPDATE dbo.Booking SET Status = 'Confirmed'
                WHERE BookingID = ? AND Status = 'Pending'
                """, booking);
    }

    public void setDiscount(Connection c, int booking, BigDecimal discount) throws SQLException {
        execute(c, "UPDATE dbo.Booking SET DiscountApplied = ? WHERE BookingID = ?",
                discount, booking);
    }

    public void setCounterStaff(Connection c, int booking, int actor) throws SQLException {
        execute(c, """
                UPDATE dbo.Booking SET StaffID = ?, Channel = 'Counter'
                WHERE BookingID = ? AND Status = 'Pending'
                """, actor, booking);
    }

    public Integer staff(Connection c, int booking) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT StaffID FROM dbo.Booking WHERE BookingID = ?")) {
            ps.setInt(1, booking);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đơn.");
                return (Integer) rs.getObject(1);
            }
        }
    }

    public boolean ticketPaymentAllowed(Connection c, int booking) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*)
                FROM dbo.Ticket t WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.Showtime s ON s.ShowtimeID = t.ShowtimeID
                JOIN dbo.Room r ON r.RoomID = s.RoomID
                JOIN dbo.Branch b ON b.BranchID = r.BranchID
                WHERE t.BookingID = ?
                  AND (t.Status <> 'Valid' OR s.Status NOT IN ('Scheduled', 'Open')
                       OR b.Status <> 'Active'
                       OR DATEADD(SECOND,
                           DATEDIFF(SECOND, CAST('00:00:00' AS TIME), s.StartTime),
                           CAST(s.ShowDate AS DATETIME2)) <= SYSDATETIME())
                """)) {
            ps.setInt(1, booking);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) == 0; }
        }
    }

    public boolean ticketRefundAllowed(Connection c, int booking, int cutoffMinutes)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT t.Status,
                       DATEADD(SECOND,
                           DATEDIFF(SECOND, CAST('00:00:00' AS TIME), s.StartTime),
                           CAST(s.ShowDate AS DATETIME2)) AS StartsAt,
                       DATEADD(MINUTE, ?, SYSDATETIME()) AS CutoffAt
                FROM dbo.Ticket t WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.Showtime s ON s.ShowtimeID = t.ShowtimeID
                WHERE t.BookingID = ?
                """)) {
            ps.setInt(1, cutoffMinutes);
            ps.setInt(2, booking);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!"Valid".equals(rs.getString("Status"))
                            || !rs.getTimestamp("StartsAt").after(rs.getTimestamp("CutoffAt"))) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public void cancelPending(Connection c, int booking) throws SQLException {
        execute(c, """
                UPDATE dbo.Booking SET Status = 'Cancelled', CancelledAt = SYSDATETIME()
                WHERE BookingID = ? AND Status = 'Pending'
                """, booking);
        execute(c, """
                UPDATE dbo.Ticket SET Status = 'Cancelled'
                WHERE BookingID = ? AND Status = 'Valid'
                """, booking);
        execute(c, """
                UPDATE dbo.Booking_F_B SET OrderStatus = 'Cancelled'
                WHERE BookingID = ? AND OrderStatus = 'Pending'
                """, booking);
    }

    public void refund(Connection c, int booking) throws SQLException {
        if (execute(c, """
                UPDATE dbo.Payment SET GatewayStatus = 'Refunded'
                WHERE BookingID = ? AND GatewayStatus = 'Success'
                """, booking) != 1) throw new SQLException("Không cập nhật được thanh toán hoàn.");
        if (execute(c, """
                UPDATE dbo.Booking
                SET Status = 'Cancelled', CancelledAt = SYSDATETIME(),
                    RefundAmount = TotalAmount
                WHERE BookingID = ? AND Status = 'Confirmed'
                """, booking) != 1) throw new SQLException("Không cập nhật được đơn hoàn.");
        execute(c, """
                UPDATE dbo.Ticket SET Status = 'Cancelled'
                WHERE BookingID = ? AND Status = 'Valid'
                """, booking);
    }

    private int execute(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            return ps.executeUpdate();
        }
    }

    public BigDecimal currentShowtimeBase(Connection c, int showtimeId, int branchId)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT s.BasePrice
                FROM dbo.Showtime s WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.Room r WITH (HOLDLOCK) ON r.RoomID = s.RoomID
                JOIN dbo.Branch b WITH (HOLDLOCK) ON b.BranchID = r.BranchID
                WHERE s.ShowtimeID = ? AND b.BranchID = ? AND b.Status = 'Active'
                  AND s.Status IN ('Open', 'Scheduled')
                  AND DATEADD(SECOND,
                        DATEDIFF(SECOND, CAST('00:00:00' AS TIME), s.StartTime),
                        CAST(s.ShowDate AS DATETIME2)) > SYSDATETIME()
                """)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }
    public model.booking.Seat currentSeat(Connection c, int showtimeId, int branchId, int seatId)
            throws SQLException {
        model.booking.Seat seat = null;
        int typeId = 0;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT se.SeatID, se.Status, sty.SeatTypeID, ISNULL(sty.Surcharge, 0) AS Surcharge
                FROM dbo.Seat se WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.Showtime s ON s.RoomID = se.RoomID
                JOIN dbo.Room r ON r.RoomID = se.RoomID
                LEFT JOIN dbo.SeatType sty WITH (HOLDLOCK) ON sty.Name = se.SeatType
                WHERE s.ShowtimeID = ? AND se.SeatID = ? AND r.BranchID = ?
                  AND se.Status <> 'Broken'
                """)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, seatId);
            ps.setInt(3, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    seat = new model.booking.Seat();
                    seat.setSeatId(rs.getInt("SeatID"));
                    seat.setSurcharge(rs.getBigDecimal("Surcharge"));
                    typeId = rs.getInt("SeatTypeID");
                }
            }
        }
        if (seat != null) {
            BigDecimal override = dao.SeatSurchargeDao.findOverrides(c, branchId).get(typeId);
            if (override != null) seat.setSurcharge(override);
        }
        return seat;
    }

    public int capturedAfterFailure(Connection c, int booking, String reference) throws SQLException {
        return execute(c, """
                UPDATE dbo.Payment
                SET GatewayStatus = 'Success', PaymentTime = SYSDATETIME(),
                    TransactionCode = COALESCE(?, TransactionCode)
                WHERE BookingID = ? AND GatewayStatus = 'Failed'
                """, reference, booking);
    }
    public void zeroPayment(Connection c, int booking) throws SQLException {
        if (execute(c, """
                UPDATE dbo.Payment SET PaymentMethod = 'Cash',
                    TransactionCode = ?, GatewayStatus = 'Success', PaymentTime = SYSDATETIME()
                WHERE BookingID = ? AND GatewayStatus = 'Pending' AND Amount = 0
                """, "FREE-" + booking, booking) != 1) {
            throw new SQLException("Không xác nhận được đơn 0 đồng.");
        }
    }
}
