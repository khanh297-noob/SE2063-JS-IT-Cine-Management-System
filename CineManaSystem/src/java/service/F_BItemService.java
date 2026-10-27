package service;

import dao.F_BItemDAO;
import model.F_BItem;
import model.User;
import dao.DBContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class F_BItemService {
    private final F_BItemDAO itemDAO = new F_BItemDAO();

    private static final Set<String> CATEGORIES = Set.of("Popcorn", "Drink", "Combo");

    public List<F_BItem> getAllItems(User actor) throws SQLException {

        requireAdministrator(actor);

        try (Connection connection = openConnection()) {
            return itemDAO.findAll(connection);
        }
    }

    public void save(User actor, int itemId, String name, String category, BigDecimal price) throws SQLException {

        requireAdministrator(actor);

        if (itemId < 0) {
            throw new IllegalArgumentException("ID không hợp lệ.");
        }

        String normalizedName = name == null ? "" : name.trim();

        if (normalizedName.isEmpty() || normalizedName.length() > 100) {
            throw new IllegalArgumentException("Tên sản phẩm phải có từ 1 đến 100 ký tự.");
        }

        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Loại sản phẩm không hợp lệ.");
        }

        BigDecimal normalizedPrice = validatePrice(price);

        F_BItem item = new F_BItem();
        item.setItemId(itemId);
        item.setItemName(normalizedName);
        item.setCategory(category);
        item.setPrice(normalizedPrice);

        try (Connection connection = openConnection()) {
            int affected = itemId == 0 ? itemDAO.insert(connection, item) : itemDAO.update(connection, item);

            if (affected != 1) {
                throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã thay đổi.");
            }
        } catch (SQLException exception) {

            if (exception.getErrorCode() == 2601 || exception.getErrorCode() == 2627) {
                throw new IllegalArgumentException("Tên sản phẩm đã tồn tại.", exception);
            }
            throw exception;
        }
    }

    public void changeActive(User actor, int itemId, boolean active)
            throws SQLException {

        requireAdministrator(actor);

        if (itemId <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ.");
        }

        try (Connection connection = openConnection()) {
            if (itemDAO.updateActive(connection, itemId, active) != 1) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            }
        }
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Giá phải lớn hơn 0.");
        }

        try {
            BigDecimal normalized = price.setScale(2, RoundingMode.UNNECESSARY);

            if (normalized.precision() > 12) {
                throw new IllegalArgumentException("Giá vượt giới hạn DECIMAL(12,2).");
            }

            return normalized;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Giá chỉ được có tối đa 2 chữ số thập phân.");
        }
    }

    private void requireAdministrator(User actor) {
        if (actor == null || !"Active".equals(actor.getStatus()) || !"Administrator".equals(actor.getRole())) {

            throw new SecurityException("Bạn không có quyền quản lý danh mục F&B.");
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DBContext.getConnection();

        if (connection == null) {
            throw new SQLException("Không kết nối được database.");
        }

        return connection;
    }
}

