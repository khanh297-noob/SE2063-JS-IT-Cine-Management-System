package dao.f_b;



import model.F_BItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class F_BItemDAO {
    public List<F_BItem> findAll(Connection connection)
            throws SQLException {

        String sql = """
                SELECT ItemID, ItemName, Category, Price, IsActive
                FROM dbo.F_B
                ORDER BY ItemID DESC
                """;

        List<F_BItem> items = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                items.add(map(result));
            }
        }

        return items;
    }

    public F_BItem findById(Connection connection, int itemId)
            throws SQLException {

        String sql = """
                SELECT ItemID, ItemName, Category, Price, IsActive
                FROM dbo.F_B
                WHERE ItemID = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, itemId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    public int insert(Connection connection, F_BItem item)
            throws SQLException {

        String sql = """
                INSERT INTO dbo.F_B
                    (ItemName, Category, Price, IsActive)
                VALUES (?, ?, ?, 1)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setNString(1, item.getItemName());
            statement.setNString(2, item.getCategory());
            statement.setBigDecimal(3, item.getPrice());

            return statement.executeUpdate();
        }
    }

    public int update(Connection connection, F_BItem item)
            throws SQLException {

        String sql = """
                UPDATE dbo.F_B
                SET ItemName = ?, Category = ?, Price = ?
                WHERE ItemID = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setNString(1, item.getItemName());
            statement.setNString(2, item.getCategory());
            statement.setBigDecimal(3, item.getPrice());
            statement.setInt(4, item.getItemId());

            return statement.executeUpdate();
        }
    }

    public int updateActive(
            Connection connection, int itemId, boolean active)
            throws SQLException {

        String sql = """
                UPDATE dbo.F_B
                SET IsActive = ?
                WHERE ItemID = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setBoolean(1, active);
            statement.setInt(2, itemId);

            return statement.executeUpdate();
        }
    }

    public F_BItem findByIdForUpdate(Connection connection, int itemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ItemID, ItemName, Category, Price, IsActive
                FROM dbo.F_B WITH (UPDLOCK, HOLDLOCK) WHERE ItemID = ?
                """)) {
            statement.setInt(1, itemId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    public int stopAllBranches(Connection connection, int itemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE dbo.Branch_F_B SET Status = 'Inactive' WHERE ItemID = ?
                """)) {
            statement.setInt(1, itemId);
            return statement.executeUpdate();
        }
    }

    private F_BItem map(ResultSet result) throws SQLException {
        F_BItem item = new F_BItem();

        item.setItemId(result.getInt("ItemID"));
        item.setItemName(result.getNString("ItemName"));
        item.setCategory(result.getNString("Category"));
        item.setPrice(result.getBigDecimal("Price"));
        item.setActive(result.getBoolean("IsActive"));

        return item;
    }
}
