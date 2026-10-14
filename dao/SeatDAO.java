package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Seat;
import cinemanagement.util.DBContext;

public class SeatDAO {

    public List<Seat> findByRoom(int roomId) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            return findByRoom(c, roomId);
        }
    }

    public List<Seat> findByRoom(Connection c, int roomId) throws SQLException {
        List<Seat> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM Seat WHERE RoomID = ? ORDER BY RowLabel, SeatNumber")) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setSeatId(rs.getInt("SeatID"));
                    s.setRoomId(rs.getInt("RoomID"));
                    s.setRowLabel(rs.getString("RowLabel"));
                    s.setSeatNumber(rs.getInt("SeatNumber"));
                    s.setSeatType(rs.getString("SeatType"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
