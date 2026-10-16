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

    // Các món đang bán (giá > 0), combo lên đầu
    public List<FnbItem> getAll() {
        List<FnbItem> list = new ArrayList<>();
        String sql = "SELECT ItemID, ItemName, Category, Price FROM F_B WHERE Price > 0 "
                + "ORDER BY CASE Category WHEN 'Combo' THEN 0 WHEN 'Popcorn' THEN 1 ELSE 2 END, ItemName";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapItem(rs));
            }
        } catch (SQLException e) {
            System.out.println("Lỗi FnbDAO.getAll: " + e.getMessage());
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
