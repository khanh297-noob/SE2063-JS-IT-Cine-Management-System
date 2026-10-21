package dao.booking;

import model.booking.Ticket;
import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO extends DBContext {

    private static final String SELECT_TICKET
            = "SELECT t.TicketID, t.BookingID, t.TicketCode, t.SeatID, t.FinalPrice, t.Status, "
            + "       s.RowLabel, s.SeatNumber, s.SeatType, "
            + "       st.ShowtimeID, st.ShowDate, st.StartTime, st.EndTime, "
            + "       m.Title, m.AgeRating, r.RoomName, r.RoomType, br.BranchID, br.Name AS BranchName, "
            + "       b.Status AS BookingStatus "
            + "FROM Ticket t "
            + "JOIN Seat s ON s.SeatID = t.SeatID "
            + "JOIN Showtime st ON st.ShowtimeID = t.ShowtimeID "
            + "JOIN Movie m ON m.MovieID = st.MovieID "
            + "JOIN Room r ON r.RoomID = st.RoomID "
            + "JOIN Branch br ON br.BranchID = r.BranchID "
            + "JOIN Booking b ON b.BookingID = t.BookingID ";

    // Tìm vé theo mã QR (TicketCode)
    public Ticket getByCode(String ticketCode) {
        String sql = SELECT_TICKET + "WHERE t.TicketCode = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ticketCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTicket(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getByCode: " + e.getMessage());
        }
        return null;
    }

    public Ticket getById(int ticketId) {
        String sql = SELECT_TICKET + "WHERE t.TicketID = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTicket(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi TicketDAO.getById: " + e.getMessage());
        }
        return null;
    }

    public List<Ticket> getByBooking(int bookingId) {
        List<Ticket> list = new ArrayList<>();
        String sql = SELECT_TICKET + "WHERE t.BookingID = ? ORDER BY s.RowLabel, s.SeatNumber";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTicket(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getByBooking: " + e.getMessage());
        }
        return list;
    }

    /**
     * Check-in vé. Chỉ cập nhật khi vé còn 'Valid'
     * => 2 cổng quét cùng lúc thì chỉ 1 cổng thành công.
     */
    public boolean checkIn(int ticketId) {
        String sql = "UPDATE Ticket SET Status = 'Checked-In' WHERE TicketID = ? AND Status = 'Valid'";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Lỗi checkIn: " + e.getMessage());
        }
        return false;
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setTicketId(rs.getInt("TicketID"));
        t.setBookingId(rs.getInt("BookingID"));
        t.setTicketCode(rs.getString("TicketCode"));
        t.setSeatId(rs.getInt("SeatID"));
        t.setFinalPrice(rs.getBigDecimal("FinalPrice"));
        t.setStatus(rs.getString("Status"));
        t.setRowLabel(rs.getString("RowLabel"));
        t.setSeatNumber(rs.getInt("SeatNumber"));
        t.setSeatType(rs.getString("SeatType"));
        t.setShowtimeId(rs.getInt("ShowtimeID"));
        t.setShowDate(rs.getDate("ShowDate"));
        t.setStartTime(rs.getTime("StartTime"));
        t.setEndTime(rs.getTime("EndTime"));
        t.setMovieTitle(rs.getString("Title"));
        t.setAgeRating(rs.getString("AgeRating"));
        t.setRoomName(rs.getString("RoomName"));
        t.setRoomType(rs.getString("RoomType"));
        t.setBranchId(rs.getInt("BranchID"));
        t.setBranchName(rs.getString("BranchName"));
        t.setBookingStatus(rs.getString("BookingStatus"));
        return t;
    }
}
