package dto;

import java.math.BigDecimal;

public class SalesSummary {
    private int totalTickets;
    private BigDecimal totalRevenue;
    private int totalBookings;
    private String topMovieTitle;
    private int topMovieTickets;
    private BigDecimal topMovieRevenue;
    private String topBranchName;
    private BigDecimal topBranchRevenue;

    public SalesSummary() {
        this.totalTickets = 0;
        this.totalRevenue = BigDecimal.ZERO;
        this.totalBookings = 0;
        this.topMovieTitle = "Chưa có dữ liệu";
        this.topMovieTickets = 0;
        this.topMovieRevenue = BigDecimal.ZERO;
        this.topBranchName = "Chưa có dữ liệu";
        this.topBranchRevenue = BigDecimal.ZERO;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(int totalBookings) {
        this.totalBookings = totalBookings;
    }

    public String getTopMovieTitle() {
        return topMovieTitle;
    }

    public void setTopMovieTitle(String topMovieTitle) {
        this.topMovieTitle = topMovieTitle;
    }

    public int getTopMovieTickets() {
        return topMovieTickets;
    }

    public void setTopMovieTickets(int topMovieTickets) {
        this.topMovieTickets = topMovieTickets;
    }

    public BigDecimal getTopMovieRevenue() {
        return topMovieRevenue != null ? topMovieRevenue : BigDecimal.ZERO;
    }

    public void setTopMovieRevenue(BigDecimal topMovieRevenue) {
        this.topMovieRevenue = topMovieRevenue;
    }

    public String getTopBranchName() {
        return topBranchName;
    }

    public void setTopBranchName(String topBranchName) {
        this.topBranchName = topBranchName;
    }

    public BigDecimal getTopBranchRevenue() {
        return topBranchRevenue != null ? topBranchRevenue : BigDecimal.ZERO;
    }

    public void setTopBranchRevenue(BigDecimal topBranchRevenue) {
        this.topBranchRevenue = topBranchRevenue;
    }
}
