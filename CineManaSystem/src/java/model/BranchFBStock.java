package model;

import java.math.BigDecimal;

public class BranchFBStock {
    private int branchId;
    private int itemId;
    private String itemName;
    private BigDecimal price;
    private int stockQuantity;
    private boolean available;
    private boolean itemActive;
    private java.math.BigDecimal basePrice;
    private java.math.BigDecimal overridePrice;
    private String status;

    public BranchFBStock() {

    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isItemActive() {
        return itemActive;
    }

    public void setItemActive(boolean itemActive) {
        this.itemActive = itemActive;
    }

    public java.math.BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(java.math.BigDecimal value) {
        basePrice=value;
    }

    public java.math.BigDecimal getOverridePrice() {
        return overridePrice;
    }

    public void setOverridePrice(java.math.BigDecimal value) {
        overridePrice=value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status=value;
    }

    public java.math.BigDecimal getEffectivePrice() {
        return overridePrice != null ? overridePrice : basePrice;
    }
}

