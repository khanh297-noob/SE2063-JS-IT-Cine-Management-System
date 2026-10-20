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
                "SELECT se.*, sty.Surcharge, sty.Color FROM Seat se LEFT JOIN SeatType sty ON sty.Name = se.SeatType "
                + "WHERE se.RoomID = ? ORDER BY se.RowNum, se.ColNum")) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setSeatId(rs.getInt("SeatID"));
                    s.setRoomId(rs.getInt("RoomID"));
                    s.setRowLabel(rs.getString("RowLabel"));
                    s.setSeatNumber(rs.getInt("SeatNumber"));
                    s.setRowNum(rs.getInt("RowNum"));
                    s.setColNum(rs.getInt("ColNum"));
                    s.setSeatType(rs.getString("SeatType"));
                    s.setSurcharge(rs.getBigDecimal("Surcharge"));
                    s.setColor(rs.getString("Color"));
                    s.setStatus(rs.getString("Status"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
