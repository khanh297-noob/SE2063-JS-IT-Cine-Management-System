package cinemanagement.model;

import java.time.LocalTime;

public class OperatingHour {
    private final boolean closed;
    private final LocalTime openTime;
    private final LocalTime closeTime;

    public OperatingHour(boolean closed, LocalTime openTime, LocalTime closeTime) {
        this.closed = closed;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public boolean isClosed() { return closed; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }

    public boolean isOpenAt(LocalTime t) {
        if (closed || openTime == null || closeTime == null) {
            return false;
        }
        if (closeTime.isAfter(openTime)) {
            return !t.isBefore(openTime) && t.isBefore(closeTime);
        }
        return !t.isBefore(openTime) || t.isBefore(closeTime);
    }
}
