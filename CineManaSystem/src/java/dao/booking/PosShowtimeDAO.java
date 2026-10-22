package dao.booking;

import model.booking.Seat;
import model.booking.Showtime;
import util.DBContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Suất chiếu + sơ đồ ghế cho POS bán vé tại quầy và soát vé.
 * Đặt tên PosShowtimeDAO để không trùng dao.ShowtimeDAO của module Chi nhánh & Phim.
 */
public class PosShowtimeDAO extends DBContext {

    // Phụ thu loại phòng lấy từ bảng RoomType (Room.RoomType = RoomType.Name)
    private static final String SELECT_SHOWTIME
            = "SELECT st.ShowtimeID, st.MovieID, m.Title, m.AgeRating, m.Duration, "
            + "       r.RoomID, r.RoomName, r.RoomType, ISNULL(rt.Surcharge, 0) AS RoomSurcharge, "
            + "       br.BranchID, br.Name AS BranchName, "
            + "       st.ShowDate, st.StartTime, st.EndTime, st.BasePrice, st.Status "
            + "FROM Showtime st "
            + "JOIN Movie m ON m.MovieID = st.MovieID "
            + "JOIN Room r ON r.RoomID = st.RoomID "
            + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType "
            + "JOIN Branch br ON br.BranchID = r.BranchID ";

    // Suất chiếu của 1 chi nhánh trong 1 ngày, lọc theo tên phim (không lấy suất đã hủy)
    public List<Showtime> getByBranchAndDate(int branchId, Date date, String keyword) {
        List<Showtime> list = new ArrayList<>();
        String sql = SELECT_SHOWTIME
                + "WHERE br.BranchID = ? AND st.ShowDate = ? AND m.Title LIKE ? AND st.Status <> 'Cancelled' "
                + "ORDER BY m.Title, st.StartTime";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, date);
            ps.setString(3, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapShowtime(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getByBranchAndDate: " + e.getMessage());
        }
        return list;
    }

    public Showtime getById(int showtimeId) {
        String sql = SELECT_SHOWTIME + "WHERE st.ShowtimeID = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapShowtime(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi PosShowtimeDAO.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Sơ đồ ghế của 1 suất chiếu (chưa có giá, giá do PosService tính từ phụ thu).
     * state: AVAILABLE (trống) | HELD (đơn Pending hoặc đang có khách giữ trong bảng SeatHold)
     *        | BOOKED (đã bán) | BROKEN (ghế hỏng, Seat.Status = 'Broken').
     * Vé đã hủy (Ticket.Status = 'Cancelled') không chiếm ghế nữa -> ghế bán lại được.
     */
    public List<Seat> getSeats(int showtimeId, int roomId) {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT s.SeatID, s.RowLabel, s.SeatNumber, s.SeatType, s.Status AS SeatStatus, "
                + "       ISNULL(sty.Surcharge, 0) AS SeatSurcharge, "
                + "  (SELECT TOP 1 b.Status FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "   WHERE t.ShowtimeID = ? AND t.SeatID = s.SeatID AND t.Status <> 'Cancelled') AS BookingStatus, "
                + "  (SELECT COUNT(*) FROM SeatHold h "
                + "   WHERE h.ShowtimeID = ? AND h.SeatID = s.SeatID AND h.ExpiresAt > SYSDATETIME()) AS HoldCount "
                + "FROM Seat s LEFT JOIN SeatType sty ON sty.Name = s.SeatType "
                + "WHERE s.RoomID = ? ORDER BY s.RowNum, s.ColNum";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, showtimeId);
            ps.setInt(3, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setSeatId(rs.getInt("SeatID"));
                    s.setRowLabel(rs.getString("RowLabel"));
                    s.setSeatNumber(rs.getInt("SeatNumber"));
                    s.setSeatType(rs.getString("SeatType"));
                    s.setSurcharge(rs.getBigDecimal("SeatSurcharge"));
                    String bookingStatus = rs.getString("BookingStatus");
                    if ("Broken".equals(rs.getString("SeatStatus"))) {
                        s.setState("BROKEN");
                    } else if ("Confirmed".equals(bookingStatus)) {
                        s.setState("BOOKED");
                    } else if ("Pending".equals(bookingStatus) || rs.getInt("HoldCount") > 0) {
                        s.setState("HELD");
                    } else {
                        s.setState("AVAILABLE");
                    }
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getSeats: " + e.getMessage());
        }
        return list;
    }

    private Showtime mapShowtime(ResultSet rs) throws SQLException {
        Showtime s = new Showtime();
        s.setShowtimeId(rs.getInt("ShowtimeID"));
        s.setMovieId(rs.getInt("MovieID"));
        s.setMovieTitle(rs.getString("Title"));
        s.setAgeRating(rs.getString("AgeRating"));
        s.setDuration(rs.getInt("Duration"));
        s.setRoomId(rs.getInt("RoomID"));
        s.setRoomName(rs.getString("RoomName"));
        s.setRoomType(rs.getString("RoomType"));
        s.setRoomSurcharge(rs.getBigDecimal("RoomSurcharge"));
        s.setStatus(rs.getString("Status"));
        s.setBranchId(rs.getInt("BranchID"));
        s.setBranchName(rs.getString("BranchName"));
        s.setShowDate(rs.getDate("ShowDate"));
        s.setStartTime(rs.getTime("StartTime"));
        s.setEndTime(rs.getTime("EndTime"));
        s.setBasePrice(rs.getBigDecimal("BasePrice"));
        return s;
    }
}
