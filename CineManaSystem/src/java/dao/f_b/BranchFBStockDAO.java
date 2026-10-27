package dao.f_b;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.BranchFBStock;

public class BranchFBStockDAO {
    private static final String SELECT_MENU = """
            SELECT bf.BranchID, bf.ItemID, f.ItemName,
                   f.Price AS BasePrice, bf.Price AS OverridePrice,
                   bf.Status, bf.StockQuantity, f.IsActive
            FROM dbo.Branch_F_B bf
            JOIN dbo.F_B f ON f.ItemID = bf.ItemID
            """;

    public List<BranchFBStock> findForBranch(Connection c, int branchId) throws SQLException {
        List<BranchFBStock> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_MENU + " WHERE bf.BranchID = ? ORDER BY f.ItemName")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public BranchFBStock findSellableForUpdate(Connection c, int branchId, int itemId)
            throws SQLException {
        String sql = """
                SELECT bf.BranchID, bf.ItemID, f.ItemName,
                       f.Price AS BasePrice, bf.Price AS OverridePrice,
                       bf.Status, bf.StockQuantity, f.IsActive
                FROM dbo.Branch_F_B bf WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.F_B f WITH (HOLDLOCK) ON f.ItemID = bf.ItemID
                JOIN dbo.Branch b WITH (HOLDLOCK) ON b.BranchID = bf.BranchID
                WHERE bf.BranchID = ? AND bf.ItemID = ?
                  AND bf.Status = 'Available' AND f.IsActive = 1 AND b.Status = 'Active'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void configure(Connection c, int branch, int item, BigDecimal price, String status)
            throws SQLException {
        boolean exists;
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM dbo.Branch_F_B WITH (UPDLOCK, HOLDLOCK)
                WHERE BranchID = ? AND ItemID = ?
                """)) {
            ps.setInt(1, branch); ps.setInt(2, item);
            try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        String sql = exists
                ? "UPDATE dbo.Branch_F_B SET Price = ?, Status = ? WHERE BranchID = ? AND ItemID = ?"
                : "INSERT INTO dbo.Branch_F_B(Price, Status, BranchID, ItemID, StockQuantity) VALUES(?, ?, ?, ?, 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (price == null) ps.setNull(1, Types.DECIMAL); else ps.setBigDecimal(1, price);
            ps.setString(2, status); ps.setInt(3, branch); ps.setInt(4, item);
            ps.executeUpdate();
        }
    }

    public int adjustStock(Connection c, int branch, int item, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE dbo.Branch_F_B SET StockQuantity = StockQuantity + ?
                WHERE BranchID = ? AND ItemID = ?
                  AND CAST(StockQuantity AS BIGINT) + ? BETWEEN 0 AND 2147483647
                """)) {
            ps.setInt(1, delta); ps.setInt(2, branch); ps.setInt(3, item); ps.setInt(4, delta);
            return ps.executeUpdate();
        }
    }

    public int takeStock(Connection c, int branch, int item, int quantity) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE dbo.Branch_F_B SET StockQuantity = StockQuantity - ?
                WHERE BranchID = ? AND ItemID = ? AND StockQuantity >= ?
                """)) {
            ps.setInt(1, quantity); ps.setInt(2, branch); ps.setInt(3, item); ps.setInt(4, quantity);
            return ps.executeUpdate();
        }
    }

    private BranchFBStock map(ResultSet rs) throws SQLException {
        BranchFBStock item = new BranchFBStock();
        item.setBranchId(rs.getInt("BranchID"));
        item.setItemId(rs.getInt("ItemID"));
        item.setItemName(rs.getString("ItemName"));
        item.setBasePrice(rs.getBigDecimal("BasePrice"));
        item.setOverridePrice(rs.getBigDecimal("OverridePrice"));
        item.setPrice(item.getEffectivePrice());
        item.setStatus(rs.getString("Status"));
        item.setAvailable("Available".equals(item.getStatus()));
        item.setStockQuantity(rs.getInt("StockQuantity"));
        item.setItemActive(rs.getBoolean("IsActive"));
        return item;
    }
}
