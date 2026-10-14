package cinemanagement.dao;

import cinemanagement.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng Seats. */
public class SeatDAO {

    public List<Seat> findByRoom(Connection con, int roomId) throws SQLException {
        String sql = "SELECT seat_id, room_id, seat_type_id, row_num, col_num, row_label, seat_number, status "
                + "FROM Seats WHERE room_id = ? ORDER BY row_num, col_num";
        List<Seat> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setSeatId(rs.getInt("seat_id"));
                    s.setRoomId(rs.getInt("room_id"));
                    s.setSeatTypeId(rs.getInt("seat_type_id"));
                    s.setRowNum(rs.getInt("row_num"));
                    s.setColNum(rs.getInt("col_num"));
                    s.setRowLabel(rs.getString("row_label"));
                    s.setSeatNumber(rs.getInt("seat_number"));
                    s.setStatus(rs.getString("status"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    public void insert(Connection con, Seat s) throws SQLException {
        String sql = "INSERT INTO Seats (room_id, seat_type_id, row_num, col_num, row_label, seat_number, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
        String sql = "UPDATE Seats SET seat_type_id = ?, status = ? WHERE seat_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getSeatTypeId());
            ps.setString(2, s.getStatus());
            ps.setInt(3, s.getSeatId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int seatId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Seats WHERE seat_id = ?")) {
            ps.setInt(1, seatId);
            ps.executeUpdate();
        }
    }
}
