package cinemanagement.dao;

import cinemanagement.model.RoomSummaryDTO;
import cinemanagement.model.Room;
import cinemanagement.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Bảng Rooms. */
public class RoomDAO {

    private static final String SELECT =
            "SELECT r.room_id, r.branch_id, r.room_type_id, r.room_name, r.total_rows, r.total_columns, r.status ";

    /** Danh sách phòng kèm tên chi nhánh, loại phòng và số ghế đang dùng được. */
    public List<RoomSummaryDTO> findSummaries(Connection con, Integer branchId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(", b.name AS branch_name, t.name AS room_type_name, ")
                .append(" (SELECT COUNT(*) FROM Seats s WHERE s.room_id = r.room_id AND s.status = 'Active') AS seat_count ")
                .append("FROM Rooms r JOIN Branches b ON b.branch_id = r.branch_id ")
                .append("JOIN RoomTypes t ON t.room_type_id = r.room_type_id ");
        if (branchId != null) sql.append("WHERE r.branch_id = ? ");
        sql.append("ORDER BY b.name, r.room_name");

        List<RoomSummaryDTO> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (branchId != null) ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomSummaryDTO(map(rs), rs.getString("branch_name"),
                            rs.getString("room_type_name"), rs.getInt("seat_count")));
                }
            }
        }
        return list;
    }

    public Room findById(Connection con, int roomId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + "FROM Rooms r WHERE r.room_id = ?")) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsByName(Connection con, int branchId, String roomName, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM Rooms WHERE branch_id = ? AND room_name = ? AND room_id <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, roomName);
            ps.setInt(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection con, Room r) throws SQLException {
        String sql = "INSERT INTO Rooms (branch_id, room_type_id, room_name, total_rows, total_columns, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getBranchId());
            ps.setInt(2, r.getRoomTypeId());
            ps.setString(3, r.getRoomName());
            ps.setInt(4, r.getTotalRows());
            ps.setInt(5, r.getTotalColumns());
            ps.setString(6, r.getStatus());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    /** Chỉ sửa tên và loại phòng. Chi nhánh không đổi được sau khi tạo. */
    public void update(Connection con, Room r) throws SQLException {
        String sql = "UPDATE Rooms SET room_name = ?, room_type_id = ? WHERE room_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getRoomName());
            ps.setInt(2, r.getRoomTypeId());
            ps.setInt(3, r.getRoomId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int roomId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Rooms SET status = ? WHERE room_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    public void updateDimensions(Connection con, int roomId, int rows, int columns) throws SQLException {
        String sql = "UPDATE Rooms SET total_rows = ?, total_columns = ? WHERE room_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rows);
            ps.setInt(2, columns);
            ps.setInt(3, roomId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int roomId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Rooms WHERE room_id = ?")) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        }
    }

    public int countByRoomType(Connection con, int roomTypeId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Rooms WHERE room_type_id = ?", roomTypeId);
    }

    public int countByBranch(Connection con, int branchId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Rooms WHERE branch_id = ?", branchId);
    }

    private int count(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("room_id"));
        r.setBranchId(rs.getInt("branch_id"));
        r.setRoomTypeId(rs.getInt("room_type_id"));
        r.setRoomName(rs.getString("room_name"));
        r.setTotalRows(rs.getInt("total_rows"));
        r.setTotalColumns(rs.getInt("total_columns"));
        r.setStatus(rs.getString("status"));
        return r;
    }
}
