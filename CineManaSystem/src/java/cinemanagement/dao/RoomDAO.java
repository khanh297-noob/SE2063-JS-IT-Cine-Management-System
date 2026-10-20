package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Room;
import cinemanagement.util.DBContext;

public class RoomDAO {
    private static final String BASE = "SELECT r.RoomID, r.BranchID, r.RoomName, r.Capacity, r.RoomType, r.TotalRows, "
            + "r.TotalColumns, r.Status, rt.Surcharge AS RoomSurcharge, b.Name AS BranchName, "
            + "(SELECT COUNT(*) FROM Seat se WHERE se.RoomID = r.RoomID AND se.Status = 'Active') AS SeatCount "
            + "FROM Room r JOIN Branch b ON b.BranchID = r.BranchID LEFT JOIN RoomType rt ON rt.Name = r.RoomType ";

    public List<Room> findByBranch(int branchId) throws SQLException {
        List<Room> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE r.BranchID = ? ORDER BY r.RoomName")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Room findById(int id) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            return findById(c, id, false);
        }
    }

    public Room findById(Connection c, int id, boolean lock) throws SQLException {
        String sql = lock
                ? BASE.replace("FROM Room r ", "FROM Room r WITH (UPDLOCK, ROWLOCK) ") + "WHERE r.RoomID = ?"
                : BASE + "WHERE r.RoomID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("RoomID"));
        r.setBranchId(rs.getInt("BranchID"));
        r.setBranchName(rs.getString("BranchName"));
        r.setRoomName(rs.getString("RoomName"));
        r.setCapacity(rs.getInt("Capacity"));
        r.setRoomType(rs.getString("RoomType"));
        r.setRoomSurcharge(rs.getBigDecimal("RoomSurcharge"));
        r.setTotalRows(rs.getInt("TotalRows"));
        r.setTotalColumns(rs.getInt("TotalColumns"));
        r.setStatus(rs.getString("Status"));
        r.setSeatCount(rs.getInt("SeatCount"));
        return r;
    }
}
