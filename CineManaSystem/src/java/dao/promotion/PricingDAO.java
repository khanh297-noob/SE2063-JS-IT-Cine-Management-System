package dao.promotion;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Showtime;

public class PricingDAO {
    public BigDecimal subtotal(Connection c, int booking) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("""
                SELECT 1 FROM dbo.Booking_F_B
                WHERE BookingID = ? AND UnitPrice IS NULL
                  AND OrderStatus <> 'Cancelled'
                """)) {
            statement.setInt(1, booking);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new IllegalArgumentException(
                            "Đơn cũ/chưa tích hợp chưa lưu đơn giá; cần đối chiếu trước khi tính lại.");
                }
            }
        }

        String sql = """
                SELECT COALESCE((SELECT SUM(FinalPrice) FROM dbo.Ticket
                                 WHERE BookingID = ? AND Status <> 'Cancelled'), 0)
                     + COALESCE((SELECT SUM(Quantity * UnitPrice) FROM dbo.Booking_F_B
                                 WHERE BookingID = ? AND OrderStatus <> 'Cancelled'), 0)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, booking); ps.setInt(2, booking);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    public List<Showtime> listShowtimes(Connection c, int branch) throws SQLException {
        List<Showtime> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT s.ShowtimeID, r.BranchID, m.Title, r.RoomName, s.ShowDate, s.BasePrice
                FROM dbo.Showtime s
                JOIN dbo.Room r ON r.RoomID = s.RoomID
                JOIN dbo.Movie m ON m.MovieID = s.MovieID
                WHERE r.BranchID = ? ORDER BY s.ShowDate DESC, s.ShowtimeID DESC
                """)) {
            ps.setInt(1, branch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Showtime s = new Showtime();
                    s.setShowtimeId(rs.getInt("ShowtimeID")); s.setBranchId(rs.getInt("BranchID"));
                    s.setMovieTitle(rs.getString("Title")); s.setRoomName(rs.getString("RoomName"));
                    s.setShowDate(rs.getDate("ShowDate")); s.setBasePrice(rs.getBigDecimal("BasePrice"));
                    result.add(s);
                }
            }
        }
        return result;
    }

    public Integer showtimeBranchForUpdate(Connection c, int showtime) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT r.BranchID FROM dbo.Showtime s WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.Room r ON r.RoomID = s.RoomID WHERE s.ShowtimeID = ?
                """)) {
            ps.setInt(1, showtime);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    public int setTicketPrice(Connection c, int showtime, BigDecimal price) throws SQLException {
        return update(c, "UPDATE dbo.Showtime SET BasePrice = ? WHERE ShowtimeID = ?", price, showtime);
    }
    public int setFnbBase(Connection c, int item, BigDecimal price) throws SQLException {
        return update(c, "UPDATE dbo.F_B SET Price = ? WHERE ItemID = ?", price, item);
    }
    public int setFnbOverride(Connection c, int branch, int item, BigDecimal price) throws SQLException {
        return update(c, "UPDATE dbo.Branch_F_B SET Price = ? WHERE BranchID = ? AND ItemID = ?", price, branch, item);
    }

    private int update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) ps.setNull(i + 1, Types.DECIMAL);
                else ps.setObject(i + 1, values[i]);
            }
            return ps.executeUpdate();
        }
    }
}
