package model.branch;

import java.math.BigDecimal;

/** Bảng SeatTypes. */
public class SeatType {

    private int seatTypeId;
    private String name;
    private BigDecimal surcharge = BigDecimal.ZERO;
    private String color;

    /** Ghế đôi: một ghế đôi chiếm 2 ô liền nhau trong sơ đồ, nên phải xếp theo cặp. */
    public boolean isSweetbox() {
        return isCoupleName(name);
    }

    /**
     * Tên loại ghế có phải ghế đôi không: "Sweetbox", "Sweet box", "Couple", "Ghế đôi", "Double"
     * (không phân biệt hoa thường, dấu, khoảng trắng). Dùng chung cho sơ đồ ghế, đặt vé online và POS.
     */
    public static boolean isCoupleName(String name) {
        if (name == null) return false;
        String n = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase().replaceAll("[\\s_-]", "");
        return n.contains("sweetbox") || n.contains("couple") || n.contains("ghedoi") || n.contains("double");
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
