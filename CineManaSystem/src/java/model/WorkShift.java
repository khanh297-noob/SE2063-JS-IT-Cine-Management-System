package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Ca làm việc của nhân viên.
 * DB chưa có bảng ca nên ca được ghi vào AuditLog (SHIFT_START / SHIFT_END) rồi dựng lại thành object này.
 */
public class WorkShift {

    private int userId;
    private String staffName;
    private String role;
    private int branchId;
    private String branchName;
    private Timestamp startTime;
    private Timestamp endTime;          // null = ca đang mở
    private BigDecimal openingFloat;    // tiền mặt đầu ca
    private BigDecimal countedCash;     // tiền đếm thực tế khi kết ca
    private BigDecimal expectedCash;    // tiền hệ thống tính phải có khi kết ca

    public WorkShift() {
    }

    public boolean isOpen() {
        return endTime == null;
    }

    // Số giờ làm (làm tròn 2 chữ số), ca đang mở thì tính tới hiện tại
    public double getHours() {
        long end = endTime == null ? System.currentTimeMillis() : endTime.getTime();
        return Math.round((end - startTime.getTime()) / 36000.0) / 100.0;
    }

    // Chênh lệch két = tiền đếm - tiền phải có
    public BigDecimal getDifference() {
        if (countedCash == null || expectedCash == null) {
            return null;
        }
        return countedCash.subtract(expectedCash);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getOpeningFloat() {
        return openingFloat;
    }

    public void setOpeningFloat(BigDecimal openingFloat) {
        this.openingFloat = openingFloat;
    }

    public BigDecimal getCountedCash() {
        return countedCash;
    }

    public void setCountedCash(BigDecimal countedCash) {
        this.countedCash = countedCash;
    }

    public BigDecimal getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(BigDecimal expectedCash) {
        this.expectedCash = expectedCash;
    }
}
