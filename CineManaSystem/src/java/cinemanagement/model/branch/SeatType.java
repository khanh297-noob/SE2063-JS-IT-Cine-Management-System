package cinemanagement.model.branch;

import java.math.BigDecimal;

/** Bảng SeatTypes. */
public class SeatType {

    private int seatTypeId;
    private String name;
    private BigDecimal surcharge = BigDecimal.ZERO;
    private String color;

    /** Ghế đôi: tên "Sweetbox" / "Sweet box" (không phân biệt hoa thường, khoảng trắng). Phải xếp theo cặp. */
    public boolean isSweetbox() {
        return name != null && name.toLowerCase().replaceAll("[\\s_-]", "").contains("sweetbox");
    }

    public int getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(int seatTypeId) { this.seatTypeId = seatTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getSurcharge() { return surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
