package dao;

import util.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Payment;

/**
 * Bảng Payment. BookingID là UNIQUE => mỗi đơn chỉ có 1 dòng Payment,
 * thanh toán lại thì UPDATE dòng cũ.
 */
public class PaymentDAO extends DBContext {

    private static final String SELECT_PAYMENT
            = "SELECT p.PaymentID, p.BookingID, p.Amount, p.PaymentMethod, p.PaymentTime, "
            + "       p.TransactionCode, p.GatewayStatus, b.Status AS BookingStatus, b.BookingDate, "
            + "       u.FullName, u.Email, u.Role, "
            + "       (SELECT TOP 1 m.Title FROM Ticket t JOIN Showtime st ON st.ShowtimeID = t.ShowtimeID "
            + "          JOIN Movie m ON m.MovieID = st.MovieID WHERE t.BookingID = b.BookingID) AS MovieTitle, "
            + "       (SELECT TOP 1 br.Name FROM Ticket t JOIN Showtime st ON st.ShowtimeID = t.ShowtimeID "
            + "          JOIN Room r ON r.RoomID = st.RoomID JOIN Branch br ON br.BranchID = r.BranchID "
            + "          WHERE t.BookingID = b.BookingID) AS BranchName, "
            + "       (SELECT COUNT(*) FROM Ticket t WHERE t.BookingID = b.BookingID) AS TicketCount "
            + "FROM Payment p "
            + "JOIN Booking b ON b.BookingID = p.BookingID "
            + "JOIN [User] u ON u.UserID = b.UserID ";

    public Payment getById(int paymentId) {
        String sql = SELECT_PAYMENT + "WHERE p.PaymentID = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi PaymentDAO.getById: " + e.getMessage());
        }
        return null;
    }

    public Payment getByBookingId(int bookingId) {
        String sql = SELECT_PAYMENT + "WHERE p.BookingID = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getByBookingId: " + e.getMessage());
        }
        return null;
    }

    // Tạo (hoặc ghi đè) dòng Payment 'Pending' khi khách bắt đầu thanh toán online
    public boolean savePending(int bookingId, BigDecimal amount, String method, String txnCode) {
        String update = "UPDATE Payment SET Amount = ?, PaymentMethod = ?, PaymentTime = GETDATE(), "
                + "TransactionCode = ?, GatewayStatus = 'Pending' WHERE BookingID = ?";
        String insert = "INSERT INTO Payment (BookingID, Amount, PaymentMethod, PaymentTime, TransactionCode, GatewayStatus) "
                + "VALUES (?, ?, ?, GETDATE(), ?, 'Pending')";
        try (Connection con = getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(update)) {
                ps.setBigDecimal(1, amount);
                ps.setString(2, method);
                ps.setString(3, txnCode);
                ps.setInt(4, bookingId);
                if (ps.executeUpdate() > 0) {
                    return true;    // đã có dòng cũ -> cập nhật xong
                }
            }
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                ps.setInt(1, bookingId);
                ps.setBigDecimal(2, amount);
                ps.setString(3, method);
                ps.setString(4, txnCode);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Lỗi savePending: " + e.getMessage());
        }
        return false;
    }

    // Lịch sử thanh toán của khách hàng
    public List<Payment> getHistory(int userId, String status) {
        List<Payment> list = new ArrayList<>();
        String sql = SELECT_PAYMENT + "WHERE b.UserID = ? "
                + (status.isEmpty() ? "" : "AND p.GatewayStatus = ? ")
                + "ORDER BY p.PaymentTime DESC";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (!status.isEmpty()) {
                ps.setString(2, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getHistory: " + e.getMessage());
        }
        return list;
    }

    // ===================== Tìm kiếm cho trang quản lý =====================
    // Ghép điều kiện WHERE theo bộ lọc. from/to dạng yyyy-MM-dd, rỗng = không lọc.
    private String buildWhere(String keyword, String method, String status, String from, String to,
            int branchId, List<Object> params) {
        String where = "WHERE 1 = 1 ";
        if (!keyword.isEmpty()) {
            where += "AND (u.FullName LIKE ? OR u.Email LIKE ? OR p.TransactionCode LIKE ?) ";
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (!method.isEmpty()) {
            where += "AND p.PaymentMethod = ? ";
            params.add(method);
        }
        if (!status.isEmpty()) {
            where += "AND p.GatewayStatus = ? ";
            params.add(status);
        }
        if (!from.isEmpty()) {
            where += "AND CAST(p.PaymentTime AS DATE) >= ? ";
            params.add(from);
        }
        if (!to.isEmpty()) {
            where += "AND CAST(p.PaymentTime AS DATE) <= ? ";
            params.add(to);
        }
        if (branchId > 0) {
            where += "AND EXISTS (SELECT 1 FROM Ticket t JOIN Showtime st ON st.ShowtimeID = t.ShowtimeID "
                    + "JOIN Room r ON r.RoomID = st.RoomID WHERE t.BookingID = b.BookingID AND r.BranchID = ?) ";
            params.add(branchId);
        }
        return where;
    }

    public List<Payment> search(String keyword, String method, String status, String from, String to,
            int branchId, int page, int pageSize) {
        List<Payment> list = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        String sql = SELECT_PAYMENT + buildWhere(keyword, method, status, from, to, branchId, params)
                + "ORDER BY p.PaymentTime DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        params.add((page - 1) * pageSize);
        params.add(pageSize);
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPayment(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi PaymentDAO.search: " + e.getMessage());
        }
        return list;
    }

    // Đếm số giao dịch theo bộ lọc
    public int count(String keyword, String method, String status, String from, String to, int branchId) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM Payment p JOIN Booking b ON b.BookingID = p.BookingID "
                + "JOIN [User] u ON u.UserID = b.UserID "
                + buildWhere(keyword, method, status, from, to, branchId, params);
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi PaymentDAO.count: " + e.getMessage());
        }
        return 0;
    }

    // Tổng tiền các giao dịch Success theo bộ lọc
    public BigDecimal sumSuccess(String keyword, String method, String status, String from, String to, int branchId) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COALESCE(SUM(p.Amount), 0) FROM Payment p JOIN Booking b ON b.BookingID = p.BookingID "
                + "JOIN [User] u ON u.UserID = b.UserID "
                + buildWhere(keyword, method, status, from, to, branchId, params)
                + "AND p.GatewayStatus = 'Success'";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi sumSuccess: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    // ===================== Két tiền tại quầy =====================
    /**
     * Tổng tiền thu tại quầy của 1 thu ngân (Booking.StaffID) theo phương thức trong khoảng thời gian.
     * Tính cả đơn đã hoàn (Refunded) vì tiền hoàn được trừ riêng ở dòng "hoàn trả" của két.
     */
    public BigDecimal sumCounterSales(int cashierId, String method, Timestamp from, Timestamp to) {
        String sql = "SELECT COALESCE(SUM(p.Amount), 0) FROM Payment p JOIN Booking b ON b.BookingID = p.BookingID "
                + "WHERE b.StaffID = ? AND p.PaymentMethod = ? AND p.GatewayStatus IN ('Success', 'Refunded') "
                + "AND p.PaymentTime >= ? AND p.PaymentTime < ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cashierId);
            ps.setString(2, method);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi sumCounterSales: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    public int countCounterSales(int cashierId, Timestamp from, Timestamp to) {
        String sql = "SELECT COUNT(*) FROM Payment p JOIN Booking b ON b.BookingID = p.BookingID "
                + "WHERE b.StaffID = ? AND p.GatewayStatus IN ('Success', 'Refunded') "
                + "AND p.PaymentTime >= ? AND p.PaymentTime < ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cashierId);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi countCounterSales: " + e.getMessage());
        }
        return 0;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("PaymentID"));
        p.setBookingId(rs.getInt("BookingID"));
        p.setAmount(rs.getBigDecimal("Amount"));
        p.setPaymentMethod(rs.getString("PaymentMethod"));
        p.setPaymentTime(rs.getTimestamp("PaymentTime"));
        p.setTransactionCode(rs.getString("TransactionCode"));
        p.setGatewayStatus(rs.getString("GatewayStatus"));
        p.setBookingStatus(rs.getString("BookingStatus"));
        p.setBookingDate(rs.getTimestamp("BookingDate"));
        p.setCustomerName(rs.getString("FullName"));
        p.setCustomerEmail(rs.getString("Email"));
        p.setCustomerRole(rs.getString("Role"));
        p.setMovieTitle(rs.getString("MovieTitle"));
        p.setBranchName(rs.getString("BranchName"));
        p.setTicketCount(rs.getInt("TicketCount"));
        return p;
    }
}
