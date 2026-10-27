package model.booking;

import java.math.BigDecimal;
import java.util.List;

import model.movie.Showtime;

public class HoldSummary {
    private final Showtime showtime;
    private final List<SeatView> seats;
    private final BigDecimal total;
    private final int remainingSeconds;

    public HoldSummary(Showtime showtime, List<SeatView> seats, BigDecimal total, int remainingSeconds) {
        this.showtime = showtime;
        this.seats = seats;
        this.total = total;
        this.remainingSeconds = remainingSeconds;
    }

    public Showtime getShowtime() { return showtime; }
    public List<SeatView> getSeats() { return seats; }
    public BigDecimal getTotal() { return total; }
    public int getRemainingSeconds() { return remainingSeconds; }

    public String getSeatCodes() {
        StringBuilder sb = new StringBuilder();
        for (SeatView s : seats) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.getCode());
        }
        return sb.toString();
    }
}
