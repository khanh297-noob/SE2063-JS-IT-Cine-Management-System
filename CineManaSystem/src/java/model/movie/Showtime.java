package model.movie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import util.DateTimeHelper;

public class Showtime {
    private int showtimeId;
    private int movieId;
    private String movieTitle;
    private int movieDuration;
    private String ageRating;
    private String posterUrl;
    private int roomId;
    private String roomName;
    private String roomType;
    private BigDecimal roomSurcharge;
    private int branchId;
    private String branchName;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal basePrice;
    private String status;
    private int soldCount;
    private int seatCount;

    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }
    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public int getMovieDuration() { return movieDuration; }
    public void setMovieDuration(int movieDuration) { this.movieDuration = movieDuration; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public BigDecimal getRoomSurcharge() { return roomSurcharge; }
    public void setRoomSurcharge(BigDecimal roomSurcharge) { this.roomSurcharge = roomSurcharge; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public LocalDateTime getStartAt() {
        return showDate.atTime(startTime);
    }

    public LocalDateTime getEndAt() {
        LocalDateTime end = showDate.atTime(endTime);
        return endTime.isAfter(startTime) ? end : end.plusDays(1);
    }

    public boolean isOvernight() {
        return !endTime.isAfter(startTime);
    }

    public boolean isCancelled() {
        return "Cancelled".equals(status);
    }

    public boolean isStarted() {
        return !LocalDateTime.now().isBefore(getStartAt());
    }

    public boolean isFinished() {
        return !LocalDateTime.now().isBefore(getEndAt());
    }

    public boolean isBookable() {
        return "Open".equals(status) && !isStarted();
    }

    public int getAvailableCount() {
        return Math.max(0, seatCount - soldCount);
    }

    public int getOccupancy() {
        return seatCount == 0 ? 0 : (int) Math.round(soldCount * 100.0 / seatCount);
    }

    public String getDateStr() { return DateTimeHelper.format(showDate); }
    public String getDateIso() { return showDate == null ? "" : showDate.toString(); }
    public String getStartStr() { return DateTimeHelper.format(startTime); }
    public String getEndStr() { return DateTimeHelper.format(endTime); }
    public String getWeekday() { return showDate == null ? "" : DateTimeHelper.weekday(showDate); }

    public String getDisplayStatus() {
        if (isCancelled()) return "Đã hủy";
        if (isFinished()) return "Đã chiếu";
        if (isStarted()) return "Đang chiếu";
        switch (status) {
            case "Scheduled": return "Chưa mở bán";
            case "Open": return "Đang mở bán";
            case "Closed": return "Ngừng bán";
            default: return status;
        }
    }

    public String getStatusClass() {
        if (isCancelled()) return "cancelled";
        if (isFinished()) return "finished";
        if (isStarted()) return "running";
        return status == null ? "" : status.toLowerCase();
    }
}
