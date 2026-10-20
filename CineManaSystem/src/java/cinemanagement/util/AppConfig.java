package cinemanagement.util;

public final class AppConfig {
    public static final int SEAT_HOLD_MINUTES = 10;
    public static final int CLEANING_BUFFER_MINUTES = 15;
    public static final int CANCEL_CUTOFF_HOURS = 2;
    public static final int MAX_SEATS_PER_BOOKING = 8;

    private AppConfig() {
    }

    public static int seatHoldMinutes() {
        return SEAT_HOLD_MINUTES;
    }

    public static int cleaningBufferMinutes() {
        return CLEANING_BUFFER_MINUTES;
    }

    public static int cancelCutoffHours() {
        return CANCEL_CUTOFF_HOURS;
    }

    public static int maxSeatsPerBooking() {
        return MAX_SEATS_PER_BOOKING;
    }
}
