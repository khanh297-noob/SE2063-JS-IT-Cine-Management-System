package cinemanagement.model;

import java.math.BigDecimal;

public class SeatView {
    public static final String AVAILABLE = "available";
    public static final String SOLD = "sold";
    public static final String HELD = "held";
    public static final String MINE = "mine";
    public static final String BROKEN = "broken";

    private final Seat seat;
    private final String state;
    private final BigDecimal price;

    public SeatView(Seat seat, String state, BigDecimal price) {
        this.seat = seat;
        this.state = state;
        this.price = price;
    }

    public Seat getSeat() { return seat; }
    public String getState() { return state; }
    public BigDecimal getPrice() { return price; }
    public int getSeatId() { return seat.getSeatId(); }
    public String getCode() { return seat.getCode(); }
    public String getSeatType() { return seat.getSeatType(); }
    public String getColor() { return seat.getColor(); }
    public int getColNum() { return seat.getColNum(); }
    public boolean isSelectable() { return AVAILABLE.equals(state) || MINE.equals(state); }
    public boolean isBroken() { return BROKEN.equals(state); }
    public boolean isMine() { return MINE.equals(state); }
}
