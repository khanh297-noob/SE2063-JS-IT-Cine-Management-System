package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class F_BOrder {
    private int bookingId;
    private int itemId;
    private int quantity;

    private String itemName;
    private BigDecimal unitPrice;
    private String orderStatus;
    private Timestamp orderDate;


    public F_BOrder() {
    }

    public F_BOrder(int bookingId, int itemId, int quantity) {
        this.bookingId = bookingId;
        this.itemId = itemId;
        this.quantity = quantity;
    }


    public F_BOrder(int bookingId, int itemId, int quantity, String itemName, BigDecimal unitPrice, String orderStatus, Timestamp orderDate) {
        this.bookingId = bookingId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
    }

    public BigDecimal getSubTotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
}