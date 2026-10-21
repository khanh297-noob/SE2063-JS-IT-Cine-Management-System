package dao.branch;

import model.branch.BranchOperatingHour;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng BranchOperatingHour. */
public class BranchOperatingHourDAO {

    public List<BranchOperatingHour> findByBranch(Connection con, int branchId) throws SQLException {
        String sql = "SELECT BranchID, DayOfWeek, IsClosed, OpenTime, CloseTime "
                + "FROM BranchOperatingHour WHERE BranchID = ? ORDER BY DayOfWeek";
        List<BranchOperatingHour> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BranchOperatingHour h = new BranchOperatingHour();
                    h.setBranchId(rs.getInt("BranchID"));
                    h.setDayOfWeek(rs.getInt("DayOfWeek"));
                    h.setClosed(rs.getBoolean("IsClosed"));
                    h.setOpenTime(JdbcUtil.getTime(rs, "OpenTime"));
                    h.setCloseTime(JdbcUtil.getTime(rs, "CloseTime"));
                    list.add(h);
                }
            }
        }
        return list;
    }

    /** Thay toàn bộ lịch tuần của chi nhánh. Gọi trong transaction. */
    public void replaceForBranch(Connection con, int branchId, List<BranchOperatingHour> hours) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM BranchOperatingHour WHERE BranchID = ?")) {
            del.setInt(1, branchId);
            del.executeUpdate();
        }
        String sql = "INSERT INTO BranchOperatingHour (BranchID, DayOfWeek, IsClosed, OpenTime, CloseTime) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (BranchOperatingHour h : hours) {
                ps.setInt(1, branchId);
                ps.setInt(2, h.getDayOfWeek());
                ps.setBoolean(3, h.isClosed());
                JdbcUtil.setTime(ps, 4, h.isClosed() ? null : h.getOpenTime());
                JdbcUtil.setTime(ps, 5, h.isClosed() ? null : h.getCloseTime());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
