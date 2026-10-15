package dto;

import java.math.BigDecimal;

/** Một dòng trong bảng phân rã doanh thu (theo phim / loại ghế / phương thức thanh toán). */
public class RevenueBreakdownRow {

    private int id;
    private String name;
    private int tickets;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private BigDecimal fnbRevenue = BigDecimal.ZERO;
    private double share;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTickets() { return tickets; }
    public void setTickets(int tickets) { this.tickets = tickets; }

    public BigDecimal getTicketRevenue() { return ticketRevenue; }
    public void setTicketRevenue(BigDecimal v) { this.ticketRevenue = v != null ? v : BigDecimal.ZERO; }

    public BigDecimal getFnbRevenue() { return fnbRevenue; }
    public void setFnbRevenue(BigDecimal v) { this.fnbRevenue = v != null ? v : BigDecimal.ZERO; }

    public BigDecimal getTotalRevenue() { return ticketRevenue.add(fnbRevenue); }

    /** Tỷ trọng (%) trong tổng doanh thu của bảng. */
    public double getShare() { return share; }
    public void setShare(double share) { this.share = share; }
}
