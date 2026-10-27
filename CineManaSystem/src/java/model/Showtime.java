package model;

public class Showtime {
    private int showtimeId;
    private int branchId;
    private String movieTitle;
    private String roomName;
    private java.sql.Date showDate;
    private java.math.BigDecimal basePrice;

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public java.sql.Date getShowDate() {
        return showDate;
    }

    public void setShowDate(java.sql.Date showDate) {
        this.showDate = showDate;
    }

    public java.math.BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(java.math.BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
}
