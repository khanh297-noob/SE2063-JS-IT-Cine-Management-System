package model.booking;

import java.math.BigDecimal;

public class SeatView {

    public static final String AVAILABLE = "available";
    public static final String SOLD = "sold";
    public static final String HELD = "held";
    public static final String MINE = "mine";
    public static final String BROKEN = "broken";

    private int seatId;
    private String rowLabel;
    private int seatNumber;
    private int rowNum;
    private int colNum;
    private String seatType;
    private String color;
    private BigDecimal surcharge = BigDecimal.ZERO;
    private boolean broken;
    private String state = AVAILABLE;
    private BigDecimal price = BigDecimal.ZERO;
    /** Ghế đôi: ô còn lại của cặp (null nếu không phải ghế đôi). */
    private SeatView pair;

    public String getCode() { return rowLabel + seatNumber; }
    public boolean isSelectable() {
        if (MINE.equals(state)) return true;
        if (!AVAILABLE.equals(state)) return false;
        // Ghế đôi chỉ chọn được khi cả 2 chỗ đều trống
        return pair == null || AVAILABLE.equals(pair.state) || MINE.equals(pair.state);
    }

    /** Ghế đôi: ô bên trái của cặp, hiển thị thành 1 ghế rộng 2 chỗ. */
    public boolean isCoupleLeft() { return pair != null && colNum < pair.colNum; }
    /** Ghế đôi: ô bên phải, đã được vẽ chung với ô bên trái nên bỏ qua khi vẽ. */
    public boolean isCoupleRight() { return pair != null && colNum > pair.colNum; }
    /** Mã hiển thị: "A1" hoặc "A1-A2" với ghế đôi. */
    public String getUnitCode() {
        if (pair == null) return getCode();
        return isCoupleLeft() ? getCode() + "-" + pair.getSeatNumber() : pair.getCode() + "-" + seatNumber;
    }
    /** Trạng thái hiển thị của cả ghế đôi: ô nào bận thì cả ghế bận. */
    public String getUnitState() {
        if (pair == null || AVAILABLE.equals(pair.state) || MINE.equals(pair.state) && MINE.equals(state)) return state;
        return AVAILABLE.equals(state) || MINE.equals(state) ? pair.state : state;
    }
    public BigDecimal getUnitPrice() { return pair == null ? price : price.add(pair.price); }
    public SeatView getPair() { return pair; }
    public void setPair(SeatView pair) { this.pair = pair; }
    public boolean isMine() { return MINE.equals(state); }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public int getRowNum() { return rowNum; }
    public void setRowNum(int rowNum) { this.rowNum = rowNum; }
    public int getColNum() { return colNum; }
    public void setColNum(int colNum) { this.colNum = colNum; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public BigDecimal getSurcharge() { return surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge == null ? BigDecimal.ZERO : surcharge; }
    public boolean isBroken() { return broken; }
    public void setBroken(boolean broken) { this.broken = broken; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
