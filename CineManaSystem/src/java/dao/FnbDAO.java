package dao;

import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.FnbItem;

public class FnbDAO extends DBContext {

    /**
     * Thực đơn của 1 chi nhánh (bảng Branch_F_B): chỉ món đang bán (Status = 'Available'),
     * giá riêng của chi nhánh nếu có, không thì lấy giá gốc trong F_B. Combo lên đầu.
     */
    public List<FnbItem> getMenu(int branchId) {
        List<FnbItem> list = new ArrayList<>();
        String sql = "SELECT f.ItemID, f.ItemName, f.Category, COALESCE(bf.Price, f.Price) AS Price "
                + "FROM Branch_F_B bf JOIN F_B f ON f.ItemID = bf.ItemID "
                + "WHERE bf.BranchID = ? AND bf.Status = 'Available' AND COALESCE(bf.Price, f.Price) > 0 "
                + "ORDER BY CASE f.Category WHEN N'Combo' THEN 0 WHEN N'Popcorn' THEN 1 ELSE 2 END, f.ItemName";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapItem(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi FnbDAO.getMenu: " + e.getMessage());
        }
        return list;
    }

    private FnbItem mapItem(ResultSet rs) throws SQLException {
        FnbItem f = new FnbItem();
        f.setItemId(rs.getInt("ItemID"));
        f.setItemName(rs.getString("ItemName"));
        f.setCategory(rs.getString("Category"));
        f.setPrice(rs.getBigDecimal("Price"));
        return f;
    }
}
