package cinemanagement.model;

import java.time.LocalTime;

/** Bảng BranchOperatingHours. dayOfWeek: 1 = Thứ Hai ... 7 = Chủ Nhật. */
public class BranchOperatingHour {

    private static final String[] DAY_NAMES =
            {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

    private int branchId;
    private int dayOfWeek;
    private boolean closed;
    private LocalTime openTime;
    private LocalTime closeTime;

    public BranchOperatingHour() {
    }

    public BranchOperatingHour(int branchId, int dayOfWeek) {
        this.branchId = branchId;
        this.dayOfWeek = dayOfWeek;
    }

    public String getDayName() {
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? DAY_NAMES[dayOfWeek - 1] : "";
    }

    /** Đóng cửa sau nửa đêm (vd 08:00 - 01:00). */
    public boolean isOvernight() {
        return !closed && openTime != null && closeTime != null && closeTime.isBefore(openTime);
    }

    /** Thời điểm t (thuộc ngày này) có nằm trong giờ mở cửa không. */
    public boolean isOpenAt(LocalTime t) {
        if (closed || openTime == null || closeTime == null) return false;
        if (isOvernight()) return !t.isBefore(openTime);
        return !t.isBefore(openTime) && t.isBefore(closeTime);
    }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }
}
