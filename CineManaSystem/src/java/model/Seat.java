package model;

import java.math.BigDecimal;

/**
 * Ghế trong sơ đồ của 1 suất chiếu.
 * state: AVAILABLE (trống) | HELD (đang giữ chờ thanh toán) | BOOKED (đã bán) | BROKEN (ghế hỏng)
 */
public class Seat {

    private int seatId;
    private String rowLabel;
    private int seatNumber;
    private String seatType;
    private String state;
    private BigDecimal price;
    private BigDecimal surcharge;   // phụ thu loại ghế (bảng SeatType)
    private boolean selected;       // thu ngân đang chọn ghế này (màn bán vé)

    public Seat() {
    }

    public String getLabel() {
        return rowLabel + seatNumber;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
