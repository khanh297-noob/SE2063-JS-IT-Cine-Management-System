package service;

import dao.F_BItemDAO;
import model.F_BItem;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Set;

public class F_BItemService {
    private final F_BItemDAO itemDAO = new F_BItemDAO();

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("Popcorn", "Drink", "Combo");

    public void createItem(User actor, String itemName, String category, BigDecimal price) throws SQLException {
        if (actor == null || !"Active".equals(actor.getStatus()) || !"Administrator".equals(actor.getRole())) {
            throw new SecurityException("Ban khong co quyen truy cap danh muc F&B");
        }

        String normalizedName =
                itemName == null ? "" : itemName.trim();

        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên sản phẩm không được để trống."
            );
        }

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "Tên sản phẩm không được vượt quá 100 ký tự."
            );
        }

        if (category == null
                || !ALLOWED_CATEGORIES.contains(category)) {

            throw new IllegalArgumentException(
                    "Loại sản phẩm không hợp lệ."
            );
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Giá sản phẩm phải lớn hơn 0."
            );
        }

        BigDecimal normalizedPrice;

        try {
            normalizedPrice = price.setScale(
                    2, RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Giá chỉ được có tối đa 2 chữ số thập phân."
            );
        }

        if (normalizedPrice.precision() > 12) {
            throw new IllegalArgumentException(
                    "Giá vượt quá giới hạn cho phép."
            );
        }

        F_BItem item = new F_BItem();
        item.setItemName(normalizedName);
        item.setCategory(category);
        item.setPrice(normalizedPrice);

        if (!itemDAO.insert(item)) {
            throw new SQLException("Không thêm được sản phẩm F&B.");
        }
    }
}

