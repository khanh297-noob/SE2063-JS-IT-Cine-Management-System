package dao.movie;

import model.movie.Movie;
import model.movie.Showtime;
import model.movie.ShowtimeRoom;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowtimeDAO {

    private static final String UPCOMING = " AND s.ShowDate >= CAST(GETDATE() AS DATE) AND s.Status <> 'Cancelled'";

    public int countByMovie(Connection con, int movieId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.MovieID = ?", movieId);
    }

    public int countUpcomingByMovie(Connection con, int movieId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.MovieID = ?" + UPCOMING, movieId);
    }

    public int countByRoom(Connection con, int roomId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.RoomID = ?", roomId);
    }

    public int countUpcomingByRoom(Connection con, int roomId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s WHERE s.RoomID = ?" + UPCOMING, roomId);
    }

    public int countUpcomingByBranch(Connection con, int branchId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE r.BranchID = ?" + UPCOMING, branchId);
    }

    public Map<Integer, Integer> countUpcomingByMovieGroupedByBranch(Connection con, int movieId) throws SQLException {
        String sql = "SELECT r.BranchID, COUNT(*) FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE s.MovieID = ?" + UPCOMING + " GROUP BY r.BranchID";
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
            }
        }
        return map;
    }

    public int countUpcomingAtBranch(Connection con, int movieId, int branchId,
                                     LocalDate from, LocalDate to) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE s.MovieID = ? AND r.BranchID = ?" + UPCOMING);
        List<Object> params = new ArrayList<>(List.of(movieId, branchId));
        if (from != null && to != null) {
            sql.append(" AND (s.ShowDate < ? OR s.ShowDate > ?)");
            params.add(Date.valueOf(from));
            params.add(Date.valueOf(to));
        } else if (from != null) {
            sql.append(" AND s.ShowDate < ?");
            params.add(Date.valueOf(from));
        } else if (to != null) {
            sql.append(" AND s.ShowDate > ?");
            params.add(Date.valueOf(to));
        }
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countTickets(Connection con, int showtimeId, boolean activeOnly) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ?" + (activeOnly ? " AND Status <> 'Cancelled'" : "");
        return count(con, sql, showtimeId);
    }

    private static final String SELECT =
            "SELECT s.ShowtimeID, s.MovieID, s.RoomID, s.ShowDate, s.StartTime, s.EndTime, s.BasePrice, s.Status, "
            + "m.Title, m.Duration, m.AgeRating, m.PosterURL, r.RoomName, r.RoomType, rt.Surcharge AS RoomSurcharge, "
            + "r.BranchID, b.Name AS BranchName, "
            + "(SELECT COUNT(*) FROM Ticket t WHERE t.ShowtimeID = s.ShowtimeID AND t.Status <> 'Cancelled') AS SoldCount, "
            + "(SELECT COUNT(*) FROM Seat se WHERE se.RoomID = s.RoomID AND se.Status = 'Active') AS SeatCount "
            + "FROM Showtime s JOIN Movie m ON m.MovieID = s.MovieID "
            + "JOIN Room r ON r.RoomID = s.RoomID JOIN Branch b ON b.BranchID = r.BranchID "
            + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType ";

    public Showtime findById(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE s.ShowtimeID = ?")) {
            ps.setInt(1, id);
            List<Showtime> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public List<Showtime> search(Connection con, Integer branchId, LocalDate from, LocalDate to, Integer roomId,
                                 Integer movieId, boolean includeCancelled) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (branchId != null) {
            sql.append(" AND r.BranchID = ?");
            params.add(branchId);
        }
        if (from != null) {
            sql.append(" AND s.ShowDate >= ?");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND s.ShowDate <= ?");
            params.add(Date.valueOf(to));
        }
        if (roomId != null) {
            sql.append(" AND s.RoomID = ?");
            params.add(roomId);
        }
        if (movieId != null) {
            sql.append(" AND s.MovieID = ?");
            params.add(movieId);
        }
        if (!includeCancelled) {
            sql.append(" AND s.Status <> 'Cancelled'");
        }
        sql.append(" ORDER BY s.ShowDate, s.StartTime, r.RoomName");
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return mapList(ps);
        }
    }

    public List<Showtime> findByRoomAround(Connection con, int roomId, LocalDate date, Integer excludeId)
            throws SQLException {
        String sql = SELECT + "WHERE s.RoomID = ? AND s.ShowDate BETWEEN ? AND ? AND s.Status <> 'Cancelled'"
                + " AND s.ShowtimeID <> ? ORDER BY s.ShowDate, s.StartTime";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setDate(2, Date.valueOf(date.minusDays(1)));
            ps.setDate(3, Date.valueOf(date.plusDays(1)));
            ps.setInt(4, excludeId == null ? -1 : excludeId);
            return mapList(ps);
        }
    }

    public int insert(Connection con, Showtime s) throws SQLException {
        String sql = "INSERT INTO Showtime (MovieID, RoomID, ShowDate, StartTime, EndTime, BasePrice, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, s);
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    public void update(Connection con, Showtime s) throws SQLException {
        String sql = "UPDATE Showtime SET MovieID = ?, RoomID = ?, ShowDate = ?, StartTime = ?, EndTime = ?, "
                + "BasePrice = ?, Status = ? WHERE ShowtimeID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            fill(ps, s);
            ps.setInt(8, s.getShowtimeId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int id, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Showtime SET Status = ? WHERE ShowtimeID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Showtime WHERE ShowtimeID = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Integer> findActiveBookingIds(Connection con, int showtimeId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT DISTINCT b.BookingID FROM Booking b "
                + "JOIN Ticket t ON t.BookingID = b.BookingID "
                + "WHERE t.ShowtimeID = ? AND b.Status IN ('Pending', 'Confirmed') AND t.Status <> 'Cancelled'")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private static final String SELECT_ROOM =
            "SELECT r.RoomID, r.BranchID, b.Name AS BranchName, r.RoomName, r.RoomType, r.Status, "
            + "ISNULL(rt.Surcharge, 0) AS RoomSurcharge, "
            + "(SELECT COUNT(*) FROM Seat se WHERE se.RoomID = r.RoomID AND se.Status = 'Active') AS SeatCount "
            + "FROM Room r JOIN Branch b ON b.BranchID = r.BranchID LEFT JOIN RoomType rt ON rt.Name = r.RoomType ";

    public List<ShowtimeRoom> findRoomsByBranch(Connection con, int branchId) throws SQLException {
        List<ShowtimeRoom> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(SELECT_ROOM + "WHERE r.BranchID = ? ORDER BY r.RoomName")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRoom(rs));
            }
        }
        return list;
    }

    public ShowtimeRoom findRoom(Connection con, int roomId, boolean lock) throws SQLException {
        String sql = (lock ? SELECT_ROOM.replace("FROM Room r ", "FROM Room r WITH (UPDLOCK, ROWLOCK) ") : SELECT_ROOM)
                + "WHERE r.RoomID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRoom(rs) : null;
            }
        }
    }

    public List<Movie> findSchedulableMovies(Connection con, int branchId) throws SQLException {
        String sql = "SELECT m.MovieID, m.Title, m.Duration, m.ReleaseDate, m.AgeRating, m.Status FROM Movie m "
                + "WHERE m.Status IN ('Now Showing', 'Coming Soon') "
                + "AND EXISTS (SELECT 1 FROM MovieBranch mb WHERE mb.MovieID = m.MovieID AND mb.BranchID = ?) "
                + "ORDER BY m.Title";
        List<Movie> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movie m = new Movie();
                    m.setMovieId(rs.getInt("MovieID"));
                    m.setTitle(rs.getString("Title"));
                    m.setDuration(rs.getInt("Duration"));
                    m.setReleaseDate(JdbcUtil.getDate(rs, "ReleaseDate"));
                    m.setAgeRating(rs.getString("AgeRating"));
                    m.setStatus(rs.getString("Status"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    public List<Showtime> findOpenByMovie(Connection con, int movieId, LocalDate from, LocalDate to) throws SQLException {
        String sql = SELECT + "WHERE s.MovieID = ? AND s.ShowDate BETWEEN ? AND ? AND s.Status = 'Open' "
                + "AND r.Status = 'Active' AND b.Status = 'Active' "
                + "ORDER BY s.ShowDate, b.Name, s.StartTime";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            return mapList(ps);
        }
    }

    public List<Showtime> findBookedByUserAround(Connection con, int userId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE s.ShowtimeID IN (SELECT t.ShowtimeID FROM Ticket t "
                + "JOIN Booking bk ON bk.BookingID = t.BookingID "
                + "WHERE bk.UserID = ? AND bk.Status = 'Confirmed' AND t.Status <> 'Cancelled') "
                + "AND s.ShowDate BETWEEN ? AND ? AND s.Status <> 'Cancelled'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date.minusDays(1)));
            ps.setDate(3, Date.valueOf(date.plusDays(1)));
            return mapList(ps);
        }
    }

    private int count(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void fill(PreparedStatement ps, Showtime s) throws SQLException {
        ps.setInt(1, s.getMovieId());
        ps.setInt(2, s.getRoomId());
        ps.setDate(3, Date.valueOf(s.getShowDate()));
        ps.setTime(4, Time.valueOf(s.getStartTime()));
        ps.setTime(5, Time.valueOf(s.getEndTime()));
        ps.setBigDecimal(6, s.getBasePrice());
        ps.setString(7, s.getStatus());
    }

    private List<Showtime> mapList(PreparedStatement ps) throws SQLException {
        List<Showtime> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Showtime map(ResultSet rs) throws SQLException {
        Showtime s = new Showtime();
        s.setShowtimeId(rs.getInt("ShowtimeID"));
        s.setMovieId(rs.getInt("MovieID"));
        s.setRoomId(rs.getInt("RoomID"));
        s.setShowDate(JdbcUtil.getDate(rs, "ShowDate"));
        s.setStartTime(JdbcUtil.getTime(rs, "StartTime"));
        s.setEndTime(JdbcUtil.getTime(rs, "EndTime"));
        s.setBasePrice(rs.getBigDecimal("BasePrice"));
        s.setStatus(rs.getString("Status"));
        s.setMovieTitle(rs.getString("Title"));
        s.setMovieDuration(rs.getInt("Duration"));
        s.setAgeRating(rs.getString("AgeRating"));
        s.setPosterUrl(rs.getString("PosterURL"));
        s.setRoomName(rs.getString("RoomName"));
        s.setRoomType(rs.getString("RoomType"));
        s.setRoomSurcharge(rs.getBigDecimal("RoomSurcharge"));
        s.setBranchId(rs.getInt("BranchID"));
        s.setBranchName(rs.getString("BranchName"));
        s.setSoldCount(rs.getInt("SoldCount"));
        s.setSeatCount(rs.getInt("SeatCount"));
        return s;
    }

    private ShowtimeRoom mapRoom(ResultSet rs) throws SQLException {
        ShowtimeRoom r = new ShowtimeRoom();
        r.setRoomId(rs.getInt("RoomID"));
        r.setBranchId(rs.getInt("BranchID"));
        r.setBranchName(rs.getString("BranchName"));
        r.setRoomName(rs.getString("RoomName"));
        r.setRoomType(rs.getString("RoomType"));
        r.setStatus(rs.getString("Status"));
        r.setRoomSurcharge(rs.getBigDecimal("RoomSurcharge"));
        r.setSeatCount(rs.getInt("SeatCount"));
        return r;
    }
}
