package dto;

import java.math.BigDecimal;

/** Hoạt động đặt vé theo một khung giờ (0-23) hoặc một ngày trong tuần (1 = Thứ Hai ... 7 = Chủ Nhật). */
public class PeakRow {

    private int slot;
    private int bookings;
    private int tickets;
    private BigDecimal revenue = BigDecimal.ZERO;

    public PeakRow() { }

    public PeakRow(int slot) { this.slot = slot; }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public int getBookings() { return bookings; }
    public void setBookings(int bookings) { this.bookings = bookings; }

    public int getTickets() { return tickets; }
    public void setTickets(int tickets) { this.tickets = tickets; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue != null ? revenue : BigDecimal.ZERO; }
}
