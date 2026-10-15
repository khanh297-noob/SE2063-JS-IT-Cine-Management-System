package dao;

import model.F_BItem;
import util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class F_BItemDAO {
    public boolean insert(F_BItem item) throws SQLException {
        String sql = """
               INSERT INTO F_B (ItemName, Category, Price)
               VALUES(?, ?, ?)
               """;
        try (Connection conn = DBContext.getConnection()) {
            if (conn == null) {
                throw new SQLException("Khong connect duoc dtb");
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setNString(1, item.getItemName());
                ps.setNString(2, item.getCategory());
                ps.setBigDecimal(3, item.getPrice());

                return ps.executeUpdate() == 1;
            }
        }
    }
}
