package dao.branch;

import model.branch.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng Seat. Cột SeatType lưu TÊN loại ghế (khoá ngoại tới SeatType.Name, theo database chung của nhóm);
 * model Seat và ma trận ghế vẫn dùng seatTypeId nên SQL JOIN SeatType để đổi qua lại giữa tên và id.
 */
public class SeatDAO {

    public List<Seat> findByRoom(Connection con, int roomId) throws SQLException {
        String sql = "SELECT s.SeatID, s.RoomID, st.SeatTypeID, s.RowNum, s.ColNum, s.RowLabel, s.SeatNumber, s.Status "
                + "FROM Seat s JOIN SeatType st ON st.Name = s.SeatType WHERE s.RoomID = ? ORDER BY s.RowNum, s.ColNum";
        List<Seat> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setSeatId(rs.getInt("SeatID"));
                    s.setRoomId(rs.getInt("RoomID"));
                    s.setSeatTypeId(rs.getInt("SeatTypeID"));
                    s.setRowNum(rs.getInt("RowNum"));
                    s.setColNum(rs.getInt("ColNum"));
                    s.setRowLabel(rs.getString("RowLabel"));
                    s.setSeatNumber(rs.getInt("SeatNumber"));
                    s.setStatus(rs.getString("status"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    public void insert(Connection con, Seat s) throws SQLException {
        String sql = "INSERT INTO Seat (RoomID, SeatType, RowNum, ColNum, RowLabel, SeatNumber, Status) "
                + "VALUES (?, (SELECT Name FROM SeatType WHERE SeatTypeID = ?), ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getRoomId());
            ps.setInt(2, s.getSeatTypeId());
            ps.setInt(3, s.getRowNum());
            ps.setInt(4, s.getColNum());
            ps.setString(5, s.getRowLabel());
            ps.setInt(6, s.getSeatNumber());
            ps.setString(7, s.getStatus());
            ps.executeUpdate();
        }
    }

    public void updateTypeAndStatus(Connection con, Seat s) throws SQLException {
        String sql = "UPDATE Seat SET SeatType = (SELECT Name FROM SeatType WHERE SeatTypeID = ?), Status = ? WHERE SeatID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getSeatTypeId());
            ps.setString(2, s.getStatus());
            ps.setInt(3, s.getSeatId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int seatId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Seat WHERE SeatID = ?")) {
            ps.setInt(1, seatId);
            ps.executeUpdate();
        }
    }
}
