package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cinemanagement.model.Ticket;
import cinemanagement.util.DBContext;

public class TicketDAO {

    public Set<Integer> soldSeatIds(int showtimeId) throws SQLException {
        Set<Integer> set = new HashSet<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT SeatID FROM Ticket WHERE ShowtimeID = ? AND Status <> 'Cancelled'")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getInt(1));
                }
            }
        }
        return set;
    }

    public int countSold(Connection c, int showtimeId, List<Integer> seatIds) throws SQLException {
        if (seatIds.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ? AND Status <> 'Cancelled' AND SeatID IN (");
        for (int i = 0; i < seatIds.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(')');
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setInt(1, showtimeId);
            for (int i = 0; i < seatIds.size(); i++) {
                ps.setInt(i + 2, seatIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void insert(Connection c, Ticket t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Ticket (BookingID, ShowtimeID, SeatID, TicketCode, "
                + "FinalPrice, Status) VALUES (?, ?, ?, ?, ?, 'Valid')")) {
            ps.setInt(1, t.getBookingId());
            ps.setInt(2, t.getShowtimeId());
            ps.setInt(3, t.getSeatId());
            ps.setString(4, t.getTicketCode());
            ps.setBigDecimal(5, t.getFinalPrice());
            ps.executeUpdate();
        }
    }

    private static final String BASE = "SELECT t.*, se.RowLabel, se.SeatNumber, se.SeatType, "
            + "b.UserID AS OwnerUserID, b.Status AS BookingStatus, r.BranchID "
            + "FROM Ticket t JOIN Seat se ON se.SeatID = t.SeatID JOIN Booking b ON b.BookingID = t.BookingID "
            + "JOIN Showtime s ON s.ShowtimeID = t.ShowtimeID JOIN Room r ON r.RoomID = s.RoomID ";

    public List<Ticket> findByBooking(int bookingId) throws SQLException {
        List<Ticket> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE t.BookingID = ? ORDER BY se.RowLabel, se.SeatNumber")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Ticket findById(int ticketId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE t.TicketID = ?")) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void cancelByBooking(Connection c, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Ticket SET Status = 'Cancelled' WHERE BookingID = ? AND Status = 'Valid'")) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    public int countCheckedIn(Connection c, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM Ticket WHERE BookingID = ? AND Status = 'Checked-In'")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Ticket map(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setTicketId(rs.getInt("TicketID"));
        t.setBookingId(rs.getInt("BookingID"));
        t.setShowtimeId(rs.getInt("ShowtimeID"));
        t.setSeatId(rs.getInt("SeatID"));
        t.setSeatCode(rs.getString("RowLabel") + rs.getInt("SeatNumber"));
        t.setSeatType(rs.getString("SeatType"));
        t.setTicketCode(rs.getString("TicketCode"));
        t.setFinalPrice(rs.getBigDecimal("FinalPrice"));
        t.setStatus(rs.getString("Status"));
        t.setOwnerUserId(rs.getInt("OwnerUserID"));
        t.setBookingStatus(rs.getString("BookingStatus"));
        t.setBranchId(rs.getInt("BranchID"));
        return t;
    }
}
