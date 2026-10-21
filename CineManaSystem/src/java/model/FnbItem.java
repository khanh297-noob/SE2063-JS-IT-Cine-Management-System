package model;

import java.math.BigDecimal;

/**
 * Món đồ ăn / nước uống (bảng F_B). quantity = số lượng khi nằm trong đơn.
 */
public class FnbItem {

    private int itemId;
    private String itemName;
    private String category;
    private BigDecimal price;
    private int quantity;

    public FnbItem() {
    }

    public BigDecimal getLineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
