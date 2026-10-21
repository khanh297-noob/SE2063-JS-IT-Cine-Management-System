package dao;

import util.DBContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.Booking;
import model.FnbItem;
import model.Seat;
import model.Ticket;

/**
 * Bảng Booking (+ Ticket, Booking_F_B, Payment trong các thao tác cần transaction).
 */
public class BookingDAO extends DBContext {

    private final TicketDAO ticketDAO = new TicketDAO();

    // Lấy đầy đủ 1 đơn: khách, voucher, thanh toán, vé, đồ ăn
    public Booking getBooking(int bookingId) {
        String sql = "SELECT b.BookingID, b.UserID, b.PromoID, b.BranchID, b.BookingDate, b.TotalAmount, b.Status, "
                + "       u.FullName, u.Email, u.Phone, u.Role, pr.PromoCode, "
                + "       p.PaymentMethod, p.GatewayStatus, p.TransactionCode, p.PaymentTime "
                + "FROM Booking b "
                + "JOIN [User] u ON u.UserID = b.UserID "
                + "LEFT JOIN Promotion pr ON pr.PromoID = b.PromoID "
                + "LEFT JOIN Payment p ON p.BookingID = b.BookingID "
                + "WHERE b.BookingID = ?";
        Booking b = null;
        try (Connection con = getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        b = new Booking();
                        b.setBookingId(rs.getInt("BookingID"));
                        b.setUserId(rs.getInt("UserID"));
                        int promoId = rs.getInt("PromoID");
                        b.setPromoId(rs.wasNull() ? null : promoId);
                        b.setPromoCode(rs.getString("PromoCode"));
                        b.setBranchId(rs.getInt("BranchID"));   // 0 nếu NULL -> lấy từ vé bên dưới
                        b.setBookingDate(rs.getTimestamp("BookingDate"));
                        b.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                        b.setStatus(rs.getString("Status"));
                        b.setCustomerName(rs.getString("FullName"));
                        b.setCustomerEmail(rs.getString("Email"));
                        b.setCustomerPhone(rs.getString("Phone"));
                        b.setCustomerRole(rs.getString("Role"));
                        b.setPaymentMethod(rs.getString("PaymentMethod"));
                        b.setPaymentStatus(rs.getString("GatewayStatus"));
                        b.setTransactionCode(rs.getString("TransactionCode"));
                        b.setPaymentTime(rs.getTimestamp("PaymentTime"));
                    }
                }
            }
            if (b == null) {
                return null;
            }
            // Đồ ăn của đơn (giá theo thực đơn của chi nhánh nếu có, không thì giá gốc)
            String fnbSql = "SELECT f.ItemID, f.ItemName, f.Category, COALESCE(brf.Price, f.Price) AS Price, bf.Quantity "
                    + "FROM Booking_F_B bf JOIN F_B f ON f.ItemID = bf.ItemID "
                    + "JOIN Booking b ON b.BookingID = bf.BookingID "
                    + "LEFT JOIN Branch_F_B brf ON brf.ItemID = f.ItemID AND brf.BranchID = b.BranchID "
                    + "WHERE bf.BookingID = ?";
            try (PreparedStatement ps = con.prepareStatement(fnbSql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FnbItem f = new FnbItem();
                        f.setItemId(rs.getInt("ItemID"));
                        f.setItemName(rs.getString("ItemName"));
                        f.setCategory(rs.getString("Category"));
                        f.setPrice(rs.getBigDecimal("Price"));
                        f.setQuantity(rs.getInt("Quantity"));
                        b.getFnbItems().add(f);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getBooking: " + e.getMessage());
            return null;
        }

        // Vé của đơn + thông tin suất chiếu (lấy từ vé đầu tiên)
        List<Ticket> tickets = ticketDAO.getByBooking(bookingId);
        b.setTickets(tickets);
        if (!tickets.isEmpty()) {
            Ticket t = tickets.get(0);
            b.setMovieTitle(t.getMovieTitle());
            b.setAgeRating(t.getAgeRating());
            b.setBranchId(t.getBranchId());   // chi nhánh của phòng chiếu
            b.setBranchName(t.getBranchName());
            b.setRoomName(t.getRoomName());
            b.setRoomType(t.getRoomType());
            b.setShowDate(t.getShowDate());
            b.setStartTime(t.getStartTime());
            b.setBranchAddress(new BranchDAO().getById(t.getBranchId()).getAddress());
        }
        return b;
    }

    // Áp / gỡ voucher cho đơn đang Pending
    public boolean updateVoucher(int bookingId, Integer promoId, BigDecimal total) {
        String sql = "UPDATE Booking SET PromoID = ?, TotalAmount = ? WHERE BookingID = ? AND Status = 'Pending'";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (promoId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, promoId);
            }
            ps.setBigDecimal(2, total);
            ps.setInt(3, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi updateVoucher: " + e.getMessage());
        }
        return false;
    }

    // Các đơn Pending đã quá thời gian giữ ghế (giờ tạo đơn + holdMinutes < bây giờ)
    public List<Integer> getExpiredPendingIds(int holdMinutes) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT BookingID FROM Booking WHERE Status = 'Pending' "
                + "AND DATEADD(MINUTE, ?, BookingDate) < GETDATE()";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, holdMinutes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getExpiredPendingIds: " + e.getMessage());
        }
        return ids;
    }

    // Đơn tại quầy do 1 thu ngân bán (Booking.StaffID) trong 1 ngày
    public List<Booking> getCounterBookings(int cashierId, Date date, String status) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT BookingID FROM Booking WHERE StaffID = ? AND CAST(BookingDate AS DATE) = ? "
                + (status.isEmpty() ? "" : "AND Status = ? ")
                + "ORDER BY BookingDate DESC";
        for (int id : getIds(sql, cashierId, date, status)) {
            list.add(getBooking(id));
        }
        return list;
    }

    private List<Integer> getIds(String sql, int cashierId, Date date, String status) {
        List<Integer> ids = new ArrayList<>();
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cashierId);
            ps.setDate(2, date);
            if (!status.isEmpty()) {
                ps.setString(3, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getCounterBookings: " + e.getMessage());
        }
        return ids;
    }

    // Tra cứu đơn tại cửa soát vé: theo mã đơn / SĐT / email / mã vé (chỉ trong chi nhánh)
    public List<Booking> lookup(String keyword, int bookingId, int branchId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT DISTINCT TOP 20 b.BookingID, b.BookingDate FROM Booking b "
                + "JOIN [User] u ON u.UserID = b.UserID "
                + "JOIN Ticket t ON t.BookingID = b.BookingID "
                + "JOIN Showtime st ON st.ShowtimeID = t.ShowtimeID "
                + "JOIN Room r ON r.RoomID = st.RoomID "
                + "WHERE r.BranchID = ? AND b.Status <> 'Pending' "
                + "  AND (b.BookingID = ? OR u.Phone = ? OR u.Email = ? OR t.TicketCode = ?) "
                + "ORDER BY b.BookingDate DESC";
        List<Integer> ids = new ArrayList<>();
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, bookingId);
            ps.setString(3, keyword);
            ps.setString(4, keyword);
            ps.setString(5, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi lookup: " + e.getMessage());
        }
        for (int id : ids) {
            list.add(getBooking(id));
        }
        return list;
    }

    // ===================== Các thao tác cần TRANSACTION =====================

    /**
     * Tạo đơn tại quầy trong 1 transaction:
     * Booking + Ticket 'Valid' cho từng ghế + Booking_F_B + Payment.
     *  - Tiền mặt / thẻ (đã thu tiền): Booking 'Confirmed', Payment 'Success'
     *  - VNPay QR (khách chưa trả):   Booking 'Pending',   Payment 'Pending' (giữ ghế chờ khách trả)
     * Khách tại quầy không có tài khoản nên UserID = thu ngân; StaffID = thu ngân, Channel = 'Counter'.
     * @return BookingID, hoặc -1 nếu ghế vừa bị người khác đặt / lỗi DB
     */
    public int createCounterBooking(int cashierId, int branchId, Integer promoId, BigDecimal total, int showtimeId,
            List<Seat> seats, List<FnbItem> items, String method, String txnCode) {
        boolean paid = !"VNPay".equals(method);
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                // 1. Kiểm tra lại ghế còn trống (khóa dòng để 2 quầy không bán trùng):
                //    chưa có vé còn hiệu lực VÀ không có khách online đang giữ (bảng SeatHold)
                String checkSql = "SELECT 1 FROM Ticket WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE ShowtimeID = ? AND SeatID = ? AND Status <> 'Cancelled' "
                        + "UNION ALL "
                        + "SELECT 1 FROM SeatHold WHERE ShowtimeID = ? AND SeatID = ? AND ExpiresAt > SYSDATETIME()";
                for (Seat s : seats) {
                    try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                        ps.setInt(1, showtimeId);
                        ps.setInt(2, s.getSeatId());
                        ps.setInt(3, showtimeId);
                        ps.setInt(4, s.getSeatId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                con.rollback();
                                return -1;
                            }
                        }
                    }
                }

                // 2. Tạo Booking
                int bookingId;
                String bookingSql = "INSERT INTO Booking (UserID, PromoID, BranchID, BookingDate, TotalAmount, Status, Channel, StaffID) "
                        + "VALUES (?, ?, ?, SYSDATETIME(), ?, ?, 'Counter', ?)";
                try (PreparedStatement ps = con.prepareStatement(bookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, cashierId);
                    if (promoId == null) {
                        ps.setNull(2, Types.INTEGER);
                    } else {
                        ps.setInt(2, promoId);
                    }
                    ps.setInt(3, branchId);
                    ps.setBigDecimal(4, total);
                    ps.setString(5, paid ? "Confirmed" : "Pending");
                    ps.setInt(6, cashierId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        bookingId = rs.getInt(1);
                    }
                }

                // 3. Tạo vé cho từng ghế
                String ticketSql = "INSERT INTO Ticket (BookingID, ShowtimeID, SeatID, TicketCode, FinalPrice, Status) "
                        + "VALUES (?, ?, ?, ?, ?, 'Valid')";
                for (Seat s : seats) {
                    try (PreparedStatement ps = con.prepareStatement(ticketSql)) {
                        ps.setInt(1, bookingId);
                        ps.setInt(2, showtimeId);
                        ps.setInt(3, s.getSeatId());
                        ps.setString(4, newTicketCode());
                        ps.setBigDecimal(5, s.getPrice());
                        ps.executeUpdate();
                    }
                }

                // 4. Đồ ăn
                String fnbSql = "INSERT INTO Booking_F_B (BookingID, ItemID, Quantity) VALUES (?, ?, ?)";
                for (FnbItem f : items) {
                    try (PreparedStatement ps = con.prepareStatement(fnbSql)) {
                        ps.setInt(1, bookingId);
                        ps.setInt(2, f.getItemId());
                        ps.setInt(3, f.getQuantity());
                        ps.executeUpdate();
                    }
                }

                // 5. Thanh toán (tiền mặt / thẻ => Success luôn; VNPay => Pending chờ khách trả)
                String paySql = "INSERT INTO Payment (BookingID, Amount, PaymentMethod, PaymentTime, TransactionCode, GatewayStatus) "
                        + "VALUES (?, ?, ?, GETDATE(), ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(paySql)) {
                    ps.setInt(1, bookingId);
                    ps.setBigDecimal(2, total);
                    ps.setString(3, method);
                    ps.setString(4, txnCode);
                    ps.setString(5, paid ? "Success" : "Pending");
                    ps.executeUpdate();
                }

                con.commit();
                return bookingId;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Lỗi createCounterBooking: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối createCounterBooking: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Thanh toán online thành công: Payment Pending -> Success, Booking Pending -> Confirmed.
     * @return "OK" | "ALREADY" (đã xử lý trước đó) | "LATE" (tiền về nhưng đơn đã bị hủy do hết giờ) | "ERROR"
     */
    public String confirmPaid(int bookingId, String newTxnCode) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                String paySql = "UPDATE Payment SET GatewayStatus = 'Success', PaymentTime = GETDATE(), "
                        + "TransactionCode = COALESCE(?, TransactionCode) "
                        + "WHERE BookingID = ? AND GatewayStatus = 'Pending'";
                int payRows;
                try (PreparedStatement ps = con.prepareStatement(paySql)) {
                    ps.setString(1, newTxnCode);
                    ps.setInt(2, bookingId);
                    payRows = ps.executeUpdate();
                }
                if (payRows == 0) {
                    con.rollback();
                    return "ALREADY";
                }
                int bookingRows;
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Booking SET Status = 'Confirmed' WHERE BookingID = ? AND Status = 'Pending'")) {
                    ps.setInt(1, bookingId);
                    bookingRows = ps.executeUpdate();
                }
                con.commit();
                return bookingRows == 1 ? "OK" : "LATE";
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Lỗi confirmPaid: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối confirmPaid: " + e.getMessage());
        }
        return "ERROR";
    }

    /**
     * Hủy đơn CHƯA thanh toán (thanh toán thất bại / hết giờ giữ ghế):
     * Payment Pending -> Failed, Booking Pending -> Cancelled, vé -> Cancelled để nhả ghế.
     * (DB chỉ cấm trùng ghế với vé chưa hủy - UX_Ticket_Seat_Active - nên giữ lại vé làm lịch sử.)
     */
    public boolean cancelUnpaid(int bookingId) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Payment SET GatewayStatus = 'Failed' WHERE BookingID = ? AND GatewayStatus = 'Pending'")) {
                    ps.setInt(1, bookingId);
                    ps.executeUpdate();
                }
                int rows;
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Booking SET Status = 'Cancelled', CancelledAt = SYSDATETIME() "
                        + "WHERE BookingID = ? AND Status = 'Pending'")) {
                    ps.setInt(1, bookingId);
                    rows = ps.executeUpdate();
                }
                if (rows == 1) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE Ticket SET Status = 'Cancelled' WHERE BookingID = ? AND Status = 'Valid'")) {
                        ps.setInt(1, bookingId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return rows == 1;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Lỗi cancelUnpaid: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối cancelUnpaid: " + e.getMessage());
        }
        return false;
    }

    /**
     * Hủy đơn tại quầy đã thanh toán (hoàn tiền):
     * Booking -> Cancelled (ghi CancelledAt, RefundAmount = toàn bộ tiền), vé -> Cancelled, Payment -> Refunded.
     */
    public boolean cancelCounterBooking(int bookingId) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Booking SET Status = 'Cancelled', CancelledAt = SYSDATETIME(), RefundAmount = TotalAmount "
                        + "WHERE BookingID = ? AND Status = 'Confirmed'")) {
                    ps.setInt(1, bookingId);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Ticket SET Status = 'Cancelled' WHERE BookingID = ? AND Status = 'Valid'")) {
                    ps.setInt(1, bookingId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Payment SET GatewayStatus = 'Refunded' WHERE BookingID = ? AND GatewayStatus = 'Success'")) {
                    ps.setInt(1, bookingId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Lỗi cancelCounterBooking: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối cancelCounterBooking: " + e.getMessage());
        }
        return false;
    }

    // Mã vé ngẫu nhiên dùng làm nội dung QR, vd TK7KQ2M9XH4PZA
    private String newTicketCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        String code = "TK";
        for (int i = 0; i < 12; i++) {
            code += chars.charAt(random.nextInt(chars.length()));
        }
        return code;
    }
}
