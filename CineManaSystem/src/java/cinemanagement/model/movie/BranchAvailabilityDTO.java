package cinemanagement.model.movie;

import cinemanagement.model.branch.Branch;

import java.time.LocalDate;

/** Một dòng trên màn "Configure Available Branches". */
public class BranchAvailabilityDTO {

    private Branch branch;
    /** Trạng thái đang lưu trong DB. */
    private boolean persisted;
    /** Trạng thái hiển thị trên form (khác persisted khi form lỗi và được render lại). */
    private boolean assigned;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private int upcomingShowtimes;

    public BranchAvailabilityDTO copy() {
        BranchAvailabilityDTO c = new BranchAvailabilityDTO();
        c.branch = branch;
        c.persisted = persisted;
        c.assigned = assigned;
        c.availableFrom = availableFrom;
        c.availableTo = availableTo;
        c.upcomingShowtimes = upcomingShowtimes;
        return c;
    }

    /** Không thể gỡ vì còn suất chiếu sắp tới. */
    public boolean isLocked() { return persisted && upcomingShowtimes > 0; }

    /** Chi nhánh ngưng hoạt động chỉ được giữ hoặc gỡ, không được thêm mới. */
    public boolean isSelectable() { return persisted || branch.isActive(); }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public boolean isPersisted() { return persisted; }
    public void setPersisted(boolean persisted) { this.persisted = persisted; }
    public boolean isAssigned() { return assigned; }
    public void setAssigned(boolean assigned) { this.assigned = assigned; }
    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }
    public LocalDate getAvailableTo() { return availableTo; }
    public void setAvailableTo(LocalDate availableTo) { this.availableTo = availableTo; }
    public int getUpcomingShowtimes() { return upcomingShowtimes; }
    public void setUpcomingShowtimes(int upcomingShowtimes) { this.upcomingShowtimes = upcomingShowtimes; }
}
