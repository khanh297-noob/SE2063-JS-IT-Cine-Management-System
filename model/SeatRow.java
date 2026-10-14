package cinemanagement.model;

import java.util.ArrayList;
import java.util.List;

public class SeatRow {
    private final String label;
    private final List<SeatView> seats = new ArrayList<>();

    public SeatRow(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
    public List<SeatView> getSeats() { return seats; }
}
