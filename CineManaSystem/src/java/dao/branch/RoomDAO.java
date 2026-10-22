package dao.branch;

import model.branch.Room;
import model.branch.RoomSummaryDTO;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng Room. Cột RoomType lưu TÊN loại phòng (khoá ngoại tới RoomType.Name, theo database chung của nhóm);
 * model Room vẫn dùng roomTypeId nên SQL JOIN RoomType để đổi qua lại giữa tên và id.
 */
public class RoomDAO {

    private static final String SELECT =
            "SELECT r.RoomID, r.BranchID, rt.RoomTypeID, r.RoomName, r.TotalRows, r.TotalColumns, r.Status ";
    private static final String FROM = "FROM Room r JOIN RoomType rt ON rt.Name = r.RoomType ";
    /** Tên loại phòng theo id, dùng khi ghi. */
    private static final String TYPE_NAME = "(SELECT Name FROM RoomType WHERE RoomTypeID = ?)";

    /** Danh sách phòng kèm tên chi nhánh, loại phòng và số ghế đang dùng được. */
    public List<RoomSummaryDTO> findSummaries(Connection con, Integer branchId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(", b.Name AS BranchName, rt.Name AS RoomTypeName, ")
                .append(" (SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID AND s.status = 'Active') AS SeatCount ")
                .append(FROM).append("JOIN Branch b ON b.BranchID = r.BranchID ");
        if (branchId != null) sql.append("WHERE r.BranchID = ? ");
        sql.append("ORDER BY b.Name, r.RoomName");

        List<RoomSummaryDTO> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (branchId != null) ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomSummaryDTO(map(rs), rs.getString("BranchName"),
                            rs.getString("RoomTypeName"), rs.getInt("SeatCount")));
                }
            }
        }
        return list;
    }

    public Room findById(Connection con, int roomId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + FROM + "WHERE r.RoomID = ?")) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsByName(Connection con, int branchId, String roomName, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM Room WHERE BranchID = ? AND RoomName = ? AND RoomID <> ?";
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
        String sql = "INSERT INTO Room (BranchID, RoomType, RoomName, TotalRows, TotalColumns, Status) "
                + "VALUES (?, " + TYPE_NAME + ", ?, ?, ?, ?)";
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
        String sql = "UPDATE Room SET RoomName = ?, RoomType = " + TYPE_NAME + " WHERE RoomID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getRoomName());
            ps.setInt(2, r.getRoomTypeId());
            ps.setInt(3, r.getRoomId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int roomId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Room SET status = ? WHERE RoomID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    /** Lưu kích thước ma trận ghế và Capacity = số ghế dùng được (Active). Gọi sau khi đã lưu ghế. */
    public void updateDimensions(Connection con, int roomId, int rows, int columns) throws SQLException {
        String sql = "UPDATE Room SET TotalRows = ?, TotalColumns = ?, "
                + "Capacity = (SELECT COUNT(*) FROM Seat WHERE RoomID = ? AND Status = 'Active') WHERE RoomID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rows);
            ps.setInt(2, columns);
            ps.setInt(3, roomId);
            ps.setInt(4, roomId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection con, int roomId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Room WHERE RoomID = ?")) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        }
    }

    public int countByRoomType(Connection con, int roomTypeId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Room r JOIN RoomType rt ON rt.Name = r.RoomType WHERE rt.RoomTypeID = ?", roomTypeId);
    }

    public int countByBranch(Connection con, int branchId) throws SQLException {
        return count(con, "SELECT COUNT(*) FROM Room WHERE BranchID = ?", branchId);
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
        r.setRoomId(rs.getInt("RoomID"));
        r.setBranchId(rs.getInt("BranchID"));
        r.setRoomTypeId(rs.getInt("RoomTypeID"));
        r.setRoomName(rs.getString("RoomName"));
        r.setTotalRows(rs.getInt("TotalRows"));
        r.setTotalColumns(rs.getInt("TotalColumns"));
        r.setStatus(rs.getString("status"));
        return r;
    }
}
