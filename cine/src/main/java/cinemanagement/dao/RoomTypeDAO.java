package cinemanagement.dao;

import cinemanagement.model.RoomType;
import cinemanagement.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Bảng RoomTypes. */
public class RoomTypeDAO {

    private static final String SELECT =
            "SELECT room_type_id, name, surcharge, description, status FROM RoomTypes ";

    public List<RoomType> findAll(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY surcharge, name")) {
            return mapList(ps);
        }
    }

    public List<RoomType> findActive(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE status = 'Active' ORDER BY surcharge, name")) {
            return mapList(ps);
        }
    }

    public RoomType findById(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE room_type_id = ?")) {
            ps.setInt(1, id);
            List<RoomType> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public boolean existsByName(Connection con, String name, int excludeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM RoomTypes WHERE name = ? AND room_type_id <> ?")) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection con, RoomType t) throws SQLException {
        String sql = "INSERT INTO RoomTypes (name, surcharge, description, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName());
            ps.setBigDecimal(2, t.getSurcharge());
            JdbcUtil.setString(ps, 3, t.getDescription());
            ps.setString(4, t.getStatus());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    public void update(Connection con, RoomType t) throws SQLException {
        String sql = "UPDATE RoomTypes SET name = ?, surcharge = ?, description = ? WHERE room_type_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setBigDecimal(2, t.getSurcharge());
            JdbcUtil.setString(ps, 3, t.getDescription());
            ps.setInt(4, t.getRoomTypeId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int id, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE RoomTypes SET status = ? WHERE room_type_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM RoomTypes WHERE room_type_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private List<RoomType> mapList(PreparedStatement ps) throws SQLException {
        List<RoomType> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RoomType t = new RoomType();
                t.setRoomTypeId(rs.getInt("room_type_id"));
                t.setName(rs.getString("name"));
                t.setSurcharge(rs.getBigDecimal("surcharge"));
                t.setDescription(rs.getString("description"));
                t.setStatus(rs.getString("status"));
                list.add(t);
            }
        }
        return list;
    }
}
