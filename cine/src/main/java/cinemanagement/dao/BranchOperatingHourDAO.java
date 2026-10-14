package cinemanagement.dao;

import cinemanagement.model.BranchOperatingHour;
import cinemanagement.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng BranchOperatingHours. */
public class BranchOperatingHourDAO {

    public List<BranchOperatingHour> findByBranch(Connection con, int branchId) throws SQLException {
        String sql = "SELECT branch_id, day_of_week, is_closed, open_time, close_time "
                + "FROM BranchOperatingHours WHERE branch_id = ? ORDER BY day_of_week";
        List<BranchOperatingHour> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BranchOperatingHour h = new BranchOperatingHour();
                    h.setBranchId(rs.getInt("branch_id"));
                    h.setDayOfWeek(rs.getInt("day_of_week"));
                    h.setClosed(rs.getBoolean("is_closed"));
                    h.setOpenTime(JdbcUtil.getTime(rs, "open_time"));
                    h.setCloseTime(JdbcUtil.getTime(rs, "close_time"));
                    list.add(h);
                }
            }
        }
        return list;
    }

    /** Thay toàn bộ lịch tuần của chi nhánh. Gọi trong transaction. */
    public void replaceForBranch(Connection con, int branchId, List<BranchOperatingHour> hours) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM BranchOperatingHours WHERE branch_id = ?")) {
            del.setInt(1, branchId);
            del.executeUpdate();
        }
        String sql = "INSERT INTO BranchOperatingHours (branch_id, day_of_week, is_closed, open_time, close_time) "
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
