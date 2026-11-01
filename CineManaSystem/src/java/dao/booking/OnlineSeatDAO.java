package dao.booking;

import model.booking.SeatView;

import dao.SeatSurchargeDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OnlineSeatDAO {

    public List<SeatView> findByRoom(Connection con, int roomId) throws SQLException {
        List<SeatView> list = new ArrayList<>();
        List<Integer> seatTypeIds = new ArrayList<>();
        int branchId = 0;
        String sql = "SELECT se.SeatID, se.RowLabel, se.SeatNumber, se.RowNum, se.ColNum, se.SeatType, se.Status, "
                + "ISNULL(sty.Surcharge, 0) AS Surcharge, sty.Color, sty.SeatTypeID, rm.BranchID "
                + "FROM Seat se JOIN Room rm ON rm.RoomID = se.RoomID "
                + "LEFT JOIN SeatType sty ON sty.Name = se.SeatType "
                + "WHERE se.RoomID = ? ORDER BY se.RowNum, se.ColNum";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SeatView s = new SeatView();
                    s.setSeatId(rs.getInt("SeatID"));
                    s.setRowLabel(rs.getString("RowLabel"));
                    s.setSeatNumber(rs.getInt("SeatNumber"));
                    s.setRowNum(rs.getInt("RowNum"));
                    s.setColNum(rs.getInt("ColNum"));
                    s.setSeatType(rs.getString("SeatType"));
                    s.setSurcharge(rs.getBigDecimal("Surcharge"));
                    s.setColor(rs.getString("Color"));
                    s.setBroken("Broken".equals(rs.getString("Status")));
                    list.add(s);
                    seatTypeIds.add(rs.getInt("SeatTypeID"));
                    branchId = rs.getInt("BranchID");
                }
            }
        }
        // Chi nhánh có phụ thu loại ghế riêng thì dùng giá riêng thay cho phụ thu chung
        if (!list.isEmpty()) {
            Map<Integer, BigDecimal> overrides = SeatSurchargeDao.findOverrides(con, branchId);
            for (int i = 0; i < list.size() && !overrides.isEmpty(); i++) {
                BigDecimal own = overrides.get(seatTypeIds.get(i));
                if (own != null) {
                    list.get(i).setSurcharge(own);
                }
            }
        }
        return list;
    }

    public Set<Integer> soldSeatIds(Connection con, int showtimeId) throws SQLException {
        Set<Integer> set = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT SeatID FROM Ticket WHERE ShowtimeID = ? AND Status <> 'Cancelled'")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getInt(1));
            }
        }
        return set;
    }

    public int countSold(Connection con, int showtimeId, List<Integer> seatIds) throws SQLException {
        if (seatIds.isEmpty()) return 0;
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ? AND Status <> 'Cancelled' AND SeatID IN (");
        for (int i = 0; i < seatIds.size(); i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(')');
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setInt(1, showtimeId);
            for (int i = 0; i < seatIds.size(); i++) ps.setInt(i + 2, seatIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
