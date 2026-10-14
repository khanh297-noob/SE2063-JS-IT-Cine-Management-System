package cinemanagement.model;

import java.math.BigDecimal;

/** 1 o ghe tren so do: ghe + trang thai theo suat chieu + gia (BR-05). */
public class SeatView {
    public static final String AVAILABLE = "available";
    public static final String SOLD = "sold";
    public static final String HELD = "held";     // nguoi khac dang giu
    public static final String MINE = "mine";     // chinh minh dang giu

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
    public boolean isSelectable() { return AVAILABLE.equals(state) || MINE.equals(state); }
    public boolean isMine() { return MINE.equals(state); }
}
