package dao;

import model.Promotion;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class PromotionDAO {
    public List<Promotion> list(Connection c, Integer branch) throws SQLException {
        String sql = "SELECT p.* FROM dbo.Promotion p";
        if (branch != null) sql += """
                 WHERE NOT EXISTS(SELECT 1 FROM dbo.PromotionBranch x WHERE x.PromoID = p.PromoID)
                    OR EXISTS(SELECT 1 FROM dbo.PromotionBranch x WHERE x.PromoID = p.PromoID AND x.BranchID = ?)
                """;
        sql += " ORDER BY p.PromoID DESC";
        List<Promotion> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (branch != null) ps.setInt(1, branch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        for (Promotion p : result) {
            p.setBranchIds(branches(c, p.getPromoId()));
            p.setUsedCount(usedCount(c, p.getPromoId()));
        }
        return result;
    }

    public Promotion findById(Connection c, int id, boolean lock) throws SQLException {
        return find(c, "PromoID", id, lock);
    }
    public Promotion findByCode(Connection c, String code, boolean lock) throws SQLException {
        return find(c, "PromoCode", code, lock);
    }
    private Promotion find(Connection c, String column, Object value, boolean lock) throws SQLException {
        // column chỉ do hai phương thức nội bộ phía trên truyền, không lấy từ request.
        String sql = "SELECT * FROM dbo.Promotion" + (lock ? " WITH (UPDLOCK, HOLDLOCK)" : "")
                + " WHERE " + column + " = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public Set<Integer> branches(Connection c, int id) throws SQLException {
        Set<Integer> result = new TreeSet<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT BranchID FROM dbo.PromotionBranch WHERE PromoID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(rs.getInt(1)); }
        }
        return result;
    }
    public int usedCount(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM dbo.Booking b WHERE b.PromoID = ? AND b.Status = 'Confirmed' AND EXISTS(SELECT 1 FROM dbo.Payment p WHERE p.BookingID = b.BookingID AND p.GatewayStatus = 'Success')")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
    public java.sql.Date today(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT CAST(SYSDATETIME() AS DATE)");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getDate(1); }
    }

    public int insert(Connection c, Promotion p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO dbo.Promotion(PromoCode, DiscountAmount, MinSpend, StartDate, EndDate, UsageLimit)
                VALUES(?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getPromoCode()); ps.setBigDecimal(2, p.getDiscountAmount());
            ps.setBigDecimal(3, p.getMinSpend()); ps.setDate(4, p.getStartDate());
            ps.setDate(5, p.getEndDate()); ps.setInt(6, p.getUsageLimit()); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Không lấy được PromoID.");
                return rs.getInt(1);
            }
        }
    }
    public int save(Connection c, Promotion p) throws SQLException {
        return update(c, """
                UPDATE dbo.Promotion SET DiscountAmount = ?, MinSpend = ?, StartDate = ?, EndDate = ?, UsageLimit = ?
                WHERE PromoID = ?
                """, p.getDiscountAmount(), p.getMinSpend(), p.getStartDate(), p.getEndDate(), p.getUsageLimit(), p.getPromoId());
    }
    public void replaceBranches(Connection c, int id, Set<Integer> branches) throws SQLException {
        update(c, "DELETE FROM dbo.PromotionBranch WHERE PromoID = ?", id);
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO dbo.PromotionBranch(PromoID, BranchID) VALUES(?, ?)")) {
            for (int branch : branches) {
                ps.setInt(1, id); ps.setInt(2, branch); ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    public int setDiscount(Connection c, int id, BigDecimal amount) throws SQLException {
        return update(c, "UPDATE dbo.Promotion SET DiscountAmount = ? WHERE PromoID = ?", amount, id);
    }
    public int setLimit(Connection c, int id, int limit) throws SQLException {
        return update(c, "UPDATE dbo.Promotion SET UsageLimit = ? WHERE PromoID = ?", limit, id);
    }
    public int deleteUnused(Connection c, int id) throws SQLException {
        return update(c, """
                DELETE FROM dbo.Promotion WHERE PromoID = ?
                AND NOT EXISTS(SELECT 1 FROM dbo.Booking b WHERE b.PromoID = Promotion.PromoID)
                """, id);
    }
    public int apply(Connection c, int booking, Integer promo, BigDecimal discount, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE dbo.Booking SET PromoID = ?, DiscountApplied = ?, TotalAmount = ?
                WHERE BookingID = ? AND Status = 'Pending'
                """)) {
            if (promo == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, promo);
            ps.setBigDecimal(2, discount); ps.setBigDecimal(3, total); ps.setInt(4, booking);
            return ps.executeUpdate();
        }
    }

    private int update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            return ps.executeUpdate();
        }
    }
    private Promotion map(ResultSet rs) throws SQLException {
        return new Promotion(rs.getInt("PromoID"), rs.getString("PromoCode"),
                rs.getBigDecimal("DiscountAmount"), rs.getBigDecimal("MinSpend"),
                rs.getInt("UsageLimit"), rs.getDate("StartDate"), rs.getDate("EndDate"));
    }
}
