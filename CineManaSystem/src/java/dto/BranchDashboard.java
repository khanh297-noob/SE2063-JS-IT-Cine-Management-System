package dto;

import java.util.ArrayList;
import java.util.List;

/** Dữ liệu trang Dashboard Tổng Quan của chi nhánh (đã định dạng sẵn để hiển thị). */
public class BranchDashboard {

    private String branchPill;
    private String todayLabel;
    private String updatedAt;

    private String todayRevenue;
    private String changeText;
    /** "up", "down" hoặc "flat". */
    private String changeCss = "flat";

    private String weekRevenue;
    private String weekShareText;

    private String occupancyText;
    private String gaugeWidth = "0";
    private String occupancyCss = "bad";
    private String seatsText;
    private String roomsText;

    private List<DashboardShowtime> showtimes = new ArrayList<>();
    private List<String> roomOptions = new ArrayList<>();

    public String getBranchPill() { return branchPill; }
    public void setBranchPill(String v) { this.branchPill = v; }
    public String getTodayLabel() { return todayLabel; }
    public void setTodayLabel(String v) { this.todayLabel = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(String v) { this.todayRevenue = v; }
    public String getChangeText() { return changeText; }
    public void setChangeText(String v) { this.changeText = v; }
    public String getChangeCss() { return changeCss; }
    public void setChangeCss(String v) { this.changeCss = v; }
    public String getWeekRevenue() { return weekRevenue; }
    public void setWeekRevenue(String v) { this.weekRevenue = v; }
    public String getWeekShareText() { return weekShareText; }
    public void setWeekShareText(String v) { this.weekShareText = v; }
    public String getOccupancyText() { return occupancyText; }
    public void setOccupancyText(String v) { this.occupancyText = v; }
    public String getGaugeWidth() { return gaugeWidth; }
    public void setGaugeWidth(String v) { this.gaugeWidth = v; }
    public String getOccupancyCss() { return occupancyCss; }
    public void setOccupancyCss(String v) { this.occupancyCss = v; }
    public String getSeatsText() { return seatsText; }
    public void setSeatsText(String v) { this.seatsText = v; }
    public String getRoomsText() { return roomsText; }
    public void setRoomsText(String v) { this.roomsText = v; }
    public List<DashboardShowtime> getShowtimes() { return showtimes; }
    public void setShowtimes(List<DashboardShowtime> v) { this.showtimes = v; }
    public List<String> getRoomOptions() { return roomOptions; }
    public void setRoomOptions(List<String> v) { this.roomOptions = v; }
}
