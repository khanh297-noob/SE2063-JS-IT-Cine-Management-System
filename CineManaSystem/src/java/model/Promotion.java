package model;

import java.math.BigDecimal;
import java.sql.Date;

public class Promotion {
    private int promoId;
    private String promoCode;
    private BigDecimal discountAmount;
    private BigDecimal minSpend;
    private int usageLimit;
    private Date startDate;
    private Date endDate;

    public Promotion() {
    }

    public Promotion(int promoId, String promoCode, BigDecimal discountAmount, BigDecimal minSpend, int usageLimit, Date startDate, Date endDate) {
        this.promoId = promoId;
        this.promoCode = promoCode;
        this.discountAmount = discountAmount;
        this.minSpend = minSpend;
        this.usageLimit = usageLimit;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getPromoId() { return promoId; }
    public void setPromoId(int promoId) { this.promoId = promoId; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getMinSpend() { return minSpend; }
    public void setMinSpend(BigDecimal minSpend) { this.minSpend = minSpend; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    private java.util.Set<Integer> branchIds = new java.util.TreeSet<>();
    private int usedCount;

    public java.util.Set<Integer> getBranchIds() {
        return branchIds;
    }
    public void setBranchIds(java.util.Set<Integer> branchIds) {
        this.branchIds = branchIds;
    }
    public int getUsedCount() {
        return usedCount;
    }
    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }
}