package cinemanagement.dao;

import cinemanagement.model.SeatType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng SeatTypes. */
public class SeatTypeDAO {

    public List<SeatType> findAll(Connection con) throws SQLException {
        String sql = "SELECT seat_type_id, name, surcharge, color FROM SeatTypes ORDER BY surcharge, name";
        List<SeatType> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SeatType t = new SeatType();
                t.setSeatTypeId(rs.getInt("seat_type_id"));
                t.setName(rs.getString("name"));
                t.setSurcharge(rs.getBigDecimal("surcharge"));
                t.setColor(rs.getString("color"));
                list.add(t);
            }
        }
        return list;
    }
}
