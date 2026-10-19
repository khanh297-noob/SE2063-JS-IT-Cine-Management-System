package cinemanagement.dao;

import cinemanagement.model.SeatType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng SeatType. */
public class SeatTypeDAO {

    public List<SeatType> findAll(Connection con) throws SQLException {
        String sql = "SELECT SeatTypeID, name, surcharge, color FROM SeatType ORDER BY surcharge, name";
        List<SeatType> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SeatType t = new SeatType();
                t.setSeatTypeId(rs.getInt("SeatTypeID"));
                t.setName(rs.getString("name"));
                t.setSurcharge(rs.getBigDecimal("surcharge"));
                t.setColor(rs.getString("color"));
                list.add(t);
            }
        }
        return list;
    }
}
