package model.movie;

import java.time.LocalDateTime;
import java.util.List;

import util.DateTimeHelper;

public class OverlapResult {
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final int bufferMinutes;
    private final List<Showtime> conflicts;

    public OverlapResult(LocalDateTime startAt, LocalDateTime endAt, int bufferMinutes, List<Showtime> conflicts) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.bufferMinutes = bufferMinutes;
        this.conflicts = conflicts;
    }

    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public int getBufferMinutes() { return bufferMinutes; }
    public List<Showtime> getConflicts() { return conflicts; }
    public boolean isOk() { return conflicts.isEmpty(); }
    public String getEndStr() { return DateTimeHelper.format(endAt.toLocalTime()); }
    public boolean isNextDay() { return !endAt.toLocalDate().equals(startAt.toLocalDate()); }

    public String describe() {
        if (isOk()) {
            return "Không trùng lịch.";
        }
        StringBuilder sb = new StringBuilder("Trùng lịch phòng (cần cách nhau tối thiểu ")
                .append(bufferMinutes).append(" phút để dọn phòng): ");
        for (int i = 0; i < conflicts.size(); i++) {
            Showtime s = conflicts.get(i);
            if (i > 0) sb.append("; ");
            sb.append(s.getMovieTitle()).append(" ").append(s.getStartStr()).append("-").append(s.getEndStr())
              .append(" ").append(s.getDateStr());
        }
        return sb.toString();
    }
}
