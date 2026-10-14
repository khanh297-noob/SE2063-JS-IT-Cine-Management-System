package cinemanagement.model;

import java.util.List;

/**
 * Dữ liệu cho màn "Configure Seat Matrix".
 *
 * layout: các hàng nối bằng "/", các ô trong hàng nối bằng ",".
 *   ""      = lối đi (không có ghế)
 *   "3"     = ghế loại seat_type_id 3
 *   "3x"    = ghế loại 3 đang hỏng
 * Ví dụ 2 hàng x 4 cột: "1,1,,2/1,1x,,2"
 */
public class SeatMatrixDTO {

    private final Room room;
    private final String branchName;
    private final List<SeatType> seatTypes;
    private final int rows;
    private final int columns;
    private final String layout;
    private final int upcomingShowtimes;

    public SeatMatrixDTO(Room room, String branchName, List<SeatType> seatTypes,
                         int rows, int columns, String layout, int upcomingShowtimes) {
        this.room = room;
        this.branchName = branchName;
        this.seatTypes = seatTypes;
        this.rows = rows;
        this.columns = columns;
        this.layout = layout;
        this.upcomingShowtimes = upcomingShowtimes;
    }

    /** Còn suất chiếu sắp tới thì chỉ được đổi loại ghế / đánh dấu hỏng. */
    public boolean isStructureLocked() { return upcomingShowtimes > 0; }

    public Room getRoom() { return room; }
    public String getBranchName() { return branchName; }
    public List<SeatType> getSeatTypes() { return seatTypes; }
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public String getLayout() { return layout; }
    public int getUpcomingShowtimes() { return upcomingShowtimes; }
}
