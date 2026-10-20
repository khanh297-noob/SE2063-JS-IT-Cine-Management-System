package cinemanagement.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Booking;
import cinemanagement.model.Showtime;
import cinemanagement.util.DBContext;

public class BookingDAO {
    private static final String BASE =
            "SELECT b.BookingID, b.UserID, b.BookingDate, b.TotalAmount, b.Status, b.Channel, b.StaffID, "
            + "b.CancelledAt, b.RefundAmount, u.FullName, u.Email, u.Phone, p.PaymentMethod, p.GatewayStatus, "
            + "tk.ShowtimeID, tk.TicketCount, tk.SeatCodes, s.ShowDate, s.StartTime, s.EndTime, s.Status AS ShowtimeStatus, "
            + "s.MovieID, s.BasePrice, m.Title, m.Duration, m.AgeRating, m.PosterURL, r.RoomID, r.RoomName, r.RoomType, "
            + "rt.Surcharge AS RoomSurcharge, r.BranchID, br.Name AS BranchName "
            + "FROM Booking b JOIN [User] u ON u.UserID = b.UserID "
            + "LEFT JOIN Payment p ON p.BookingID = b.BookingID "
            + "OUTER APPLY (SELECT MIN(t.ShowtimeID) AS ShowtimeID, COUNT(*) AS TicketCount, "
            + "   STRING_AGG(CONCAT(se.RowLabel, se.SeatNumber), ', ') WITHIN GROUP (ORDER BY se.RowLabel, se.SeatNumber) AS SeatCodes "
            + "   FROM Ticket t JOIN Seat se ON se.SeatID = t.SeatID WHERE t.BookingID = b.BookingID) tk "
            + "LEFT JOIN Showtime s ON s.ShowtimeID = tk.ShowtimeID "
            + "LEFT JOIN Movie m ON m.MovieID = s.MovieID "
            + "LEFT JOIN Room r ON r.RoomID = s.RoomID "
            + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType "
            + "LEFT JOIN Branch br ON br.BranchID = r.BranchID ";

    public int insert(Connection c, Booking b) throws SQLException {
        String sql = "INSERT INTO Booking (UserID, PromoID, TotalAmount, Status, Channel, StaffID) VALUES (?, NULL, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getUserId());
            ps.setBigDecimal(2, b.getTotalAmount());
            ps.setString(3, b.getStatus());
            ps.setString(4, b.getChannel());
            if (b.getStaffId() == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, b.getStaffId());
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Booking findById(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE b.BookingID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Booking> findByUser(int userId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE b.UserID = ? ORDER BY b.BookingDate DESC")) {
            ps.setInt(1, userId);
            return list(ps);
        }
    }

    public List<Integer> findActiveBookingIdsByShowtime(Connection c, int showtimeId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT b.BookingID FROM Booking b JOIN Ticket t "
                + "ON t.BookingID = b.BookingID WHERE t.ShowtimeID = ? AND b.Status = 'Confirmed' AND t.Status <> 'Cancelled'")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    public BigDecimal lockConfirmedTotal(Connection c, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT TotalAmount FROM Booking WITH (UPDLOCK, ROWLOCK) "
                + "WHERE BookingID = ? AND Status = 'Confirmed'")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    public void markCancelled(Connection c, int bookingId, BigDecimal refund) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE Booking SET Status = 'Cancelled', CancelledAt = SYSDATETIME(), "
                + "RefundAmount = ? WHERE BookingID = ?")) {
            ps.setBigDecimal(1, refund);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    public int countUpcomingConfirmed(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT b.BookingID) FROM Booking b JOIN Ticket t ON t.BookingID = b.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = t.ShowtimeID WHERE b.UserID = ? AND b.Status = 'Confirmed' "
                + "AND t.Status = 'Valid' AND s.Status <> 'Cancelled' "
                + "AND (s.ShowDate > CAST(GETDATE() AS DATE) OR (s.ShowDate = CAST(GETDATE() AS DATE) AND s.StartTime > CAST(GETDATE() AS TIME)))";
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Booking> list(PreparedStatement ps) throws SQLException {
        List<Booking> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Booking map(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("BookingID"));
        b.setUserId(rs.getInt("UserID"));
        Timestamp bd = rs.getTimestamp("BookingDate");
        b.setBookingDate(bd == null ? null : bd.toLocalDateTime());
        b.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        b.setStatus(rs.getString("Status"));
        b.setChannel(rs.getString("Channel"));
        int staff = rs.getInt("StaffID");
        b.setStaffId(rs.wasNull() ? null : staff);
        Timestamp ca = rs.getTimestamp("CancelledAt");
        b.setCancelledAt(ca == null ? null : ca.toLocalDateTime());
        b.setRefundAmount(rs.getBigDecimal("RefundAmount"));
        b.setCustomerName(rs.getString("FullName"));
        b.setCustomerEmail(rs.getString("Email"));
        b.setCustomerPhone(rs.getString("Phone"));
        b.setPaymentMethod(rs.getString("PaymentMethod"));
        b.setPaymentStatus(rs.getString("GatewayStatus"));
        b.setTicketCount(rs.getInt("TicketCount"));
        b.setSeatCodes(rs.getString("SeatCodes"));

        int showtimeId = rs.getInt("ShowtimeID");
        if (!rs.wasNull()) {
            Showtime s = new Showtime();
            s.setShowtimeId(showtimeId);
            Date d = rs.getDate("ShowDate");
            Time st = rs.getTime("StartTime");
            Time et = rs.getTime("EndTime");
            s.setShowDate(d.toLocalDate());
            s.setStartTime(st.toLocalTime());
            s.setEndTime(et.toLocalTime());
            s.setStatus(rs.getString("ShowtimeStatus"));
            s.setMovieId(rs.getInt("MovieID"));
            s.setBasePrice(rs.getBigDecimal("BasePrice"));
            s.setMovieTitle(rs.getString("Title"));
            s.setMovieDuration(rs.getInt("Duration"));
            s.setAgeRating(rs.getString("AgeRating"));
            s.setPosterUrl(rs.getString("PosterURL"));
            s.setRoomId(rs.getInt("RoomID"));
            s.setRoomName(rs.getString("RoomName"));
            s.setRoomType(rs.getString("RoomType"));
            s.setRoomSurcharge(rs.getBigDecimal("RoomSurcharge"));
            s.setBranchId(rs.getInt("BranchID"));
            s.setBranchName(rs.getString("BranchName"));
            b.setShowtime(s);
        }
        return b;
    }
}
