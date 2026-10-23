package service.branch;

import dao.movie.ShowtimeDAO;
import dao.branch.BranchDAO;
import dao.branch.RoomDAO;
import dao.branch.SeatDAO;
import dao.branch.SeatTypeDAO;
import model.branch.Branch;
import model.branch.Room;
import model.branch.Seat;
import model.branch.SeatMatrixDTO;
import model.branch.SeatType;
import util.BusinessException;
import util.JdbcContext;
import util.JdbcUtil;
import util.NotFoundException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configure Seat Matrix.
 *
 * BR-SM-01  Ma trận từ 1x1 đến 26 hàng (A-Z) x 30 cột. Ghế lưu toạ độ row_num / col_num.
 * BR-SM-02  Phòng phải có ít nhất một ghế.
 * BR-SM-03  Khi phòng còn suất chiếu sắp tới: chỉ được đổi loại ghế hoặc đánh dấu hỏng,
 *           không thêm/bớt ghế (tránh làm lệch vé đã bán).
 * BR-SM-04  Ghế đã có vé không xoá được, chỉ đánh dấu hỏng.
 * BR-SM-05  Lối đi phải liền nhau và thông ra ít nhất một trong 4 góc phòng (xem SeatLayoutValidator).
 * BR-SM-06  Ghế Sweetbox phải đi theo cặp (xem SeatLayoutValidator).
 */
public class SeatMatrixService {

    public static final int MAX_ROWS = 26;
    public static final int MAX_COLUMNS = 30;
    private static final int DEFAULT_ROWS = 8;
    private static final int DEFAULT_COLUMNS = 12;

    private final RoomDAO roomDAO = new RoomDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final SeatTypeDAO seatTypeDAO = new SeatTypeDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    /** Số lượng thay đổi sau khi lưu. */
    public static final class SaveResult {
        private final int added;
        private final int removed;
        private final int changed;

        SaveResult(int added, int removed, int changed) {
            this.added = added;
            this.removed = removed;
            this.changed = changed;
        }

        public int getAdded() { return added; }
        public int getRemoved() { return removed; }
        public int getChanged() { return changed; }
    }

    public SeatMatrixDTO getMatrix(int roomId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Room room = roomDAO.findById(con, roomId);
            if (room == null) throw new NotFoundException("Không tìm thấy phòng #" + roomId);
            Branch branch = branchDAO.findById(con, room.getBranchId());
            List<SeatType> types = seatTypeDAO.findAll(con);
            List<Seat> seats = seatDAO.findByRoom(con, roomId);

            int rows = room.getTotalRows();
            int cols = room.getTotalColumns();
            String layout;
            if (seats.isEmpty() || rows == 0 || cols == 0) {
                // Phòng mới: gợi ý sẵn 8x12, lối đi hai bên (cột đầu, cột cuối) để hợp lệ ngay,
                // còn lại là ghế loại rẻ nhất để người dùng tô lại.
                rows = DEFAULT_ROWS;
                cols = DEFAULT_COLUMNS;
                String fill = types.isEmpty() ? "" : String.valueOf(types.get(0).getSeatTypeId());
                int lastCol = cols;
                layout = buildLayout(rows, cols, (r, c) -> c == 1 || c == lastCol ? "" : fill);
            } else {
                Map<Integer, Seat> byCoord = new HashMap<>();
                for (Seat s : seats) byCoord.put(s.getCoordKey(), s);
                layout = buildLayout(rows, cols, (r, c) -> {
                    Seat s = byCoord.get(Seat.coordKey(r + 1, c));
                    return s == null ? "" : s.getSeatTypeId() + (s.isBroken() ? "x" : "");
                });
            }
            int upcoming = showtimeDAO.countUpcomingByRoom(con, roomId);
            return new SeatMatrixDTO(room, branch == null ? "" : branch.getName(), types, rows, cols, layout, upcoming);
        });
    }

    public SaveResult save(int roomId, Integer rows, Integer cols, String layout) throws SQLException, BusinessException {
        if (rows == null || rows < 1 || rows > MAX_ROWS)
            throw new BusinessException("Số hàng phải từ 1 đến " + MAX_ROWS + ".");
        if (cols == null || cols < 1 || cols > MAX_COLUMNS)
            throw new BusinessException("Số cột phải từ 1 đến " + MAX_COLUMNS + ".");

        return JdbcContext.transaction(con -> {
            Room room = roomDAO.findById(con, roomId);
            if (room == null) throw new NotFoundException("Không tìm thấy phòng #" + roomId);

            Set<Integer> typeIds = new HashSet<>();
            Set<Integer> sweetboxIds = new HashSet<>();
            for (SeatType t : seatTypeDAO.findAll(con)) {
                typeIds.add(t.getSeatTypeId());
                if (t.isSweetbox()) sweetboxIds.add(t.getSeatTypeId());
            }

            Map<Integer, Seat> wanted = parseLayout(roomId, rows, cols, layout, typeIds);
            if (wanted.isEmpty()) throw new BusinessException("Phòng phải có ít nhất một ghế.");

            int upcoming = showtimeDAO.countUpcomingByRoom(con, roomId);
            int[][] grid = toGrid(rows, cols, wanted);
            List<String> errors = new ArrayList<>();
            // Khi phòng bị khoá (BR-SM-03) lối đi không đổi được nên không kiểm tra lại.
            if (upcoming == 0) errors.addAll(SeatLayoutValidator.checkAisles(grid));
            errors.addAll(SeatLayoutValidator.checkSweetboxPairs(grid, sweetboxIds));
            if (!errors.isEmpty()) throw new BusinessException(errors);

            Map<Integer, Seat> existing = new HashMap<>();
            for (Seat s : seatDAO.findByRoom(con, roomId)) existing.put(s.getCoordKey(), s);

            List<Seat> toInsert = new ArrayList<>();
            List<Seat> toUpdate = new ArrayList<>();
            List<Seat> toDelete = new ArrayList<>();
            for (Map.Entry<Integer, Seat> e : wanted.entrySet()) {
                Seat old = existing.get(e.getKey());
                Seat neu = e.getValue();
                if (old == null) {
                    toInsert.add(neu);
                } else if (old.getSeatTypeId() != neu.getSeatTypeId() || !old.getStatus().equals(neu.getStatus())) {
                    old.setSeatTypeId(neu.getSeatTypeId());
                    old.setStatus(neu.getStatus());
                    toUpdate.add(old);
                }
            }
            for (Map.Entry<Integer, Seat> e : existing.entrySet()) {
                if (!wanted.containsKey(e.getKey())) toDelete.add(e.getValue());
            }

            if ((!toInsert.isEmpty() || !toDelete.isEmpty()) && upcoming > 0)
                throw new BusinessException("Phòng còn " + upcoming + " suất chiếu sắp tới nên không thể "
                        + "thêm hoặc bớt ghế. Bạn vẫn có thể đổi loại ghế hoặc đánh dấu ghế hỏng.");

            for (Seat s : toDelete) {
                try {
                    seatDAO.delete(con, s.getSeatId());
                } catch (SQLException e) {
                    if (JdbcUtil.isForeignKeyViolation(e))
                        throw new BusinessException("Ghế " + s.getPosition() + " đã có vé nên không xoá được. "
                                + "Hãy đánh dấu ghế hỏng.");
                    throw e;
                }
            }
            for (Seat s : toInsert) seatDAO.insert(con, s);
            for (Seat s : toUpdate) seatDAO.updateTypeAndStatus(con, s);
            roomDAO.updateDimensions(con, roomId, rows, cols);
            return new SaveResult(toInsert.size(), toDelete.size(), toUpdate.size());
        });
    }

    // ------------------------------------------------------------------

    public static String rowLabel(int rowIndex) {
        return String.valueOf((char) ('A' + rowIndex));
    }

    private interface CellToken {
        String at(int rowIndex, int column);
    }

    private static String buildLayout(int rows, int cols, CellToken token) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            if (r > 0) sb.append('/');
            for (int c = 1; c <= cols; c++) {
                if (c > 1) sb.append(',');
                sb.append(token.at(r, c));
            }
        }
        return sb.toString();
    }

    /** Ma trận seat_type_id theo toạ độ 0-based, 0 = lối đi. */
    private static int[][] toGrid(int rows, int cols, Map<Integer, Seat> seats) {
        int[][] grid = new int[rows][cols];
        for (Seat s : seats.values()) grid[s.getRowNum() - 1][s.getColNum() - 1] = s.getSeatTypeId();
        return grid;
    }

    /** Khoá của map = Seat.coordKey(row_num, col_num). */
    private static Map<Integer, Seat> parseLayout(int roomId, int rows, int cols, String layout, Set<Integer> typeIds)
            throws BusinessException {
        if (layout == null) throw new BusinessException("Thiếu dữ liệu ma trận ghế.");
        String[] rowTokens = layout.split("/", -1);
        if (rowTokens.length != rows) throw new BusinessException("Dữ liệu ma trận không khớp số hàng. Tải lại trang.");

        Map<Integer, Seat> seats = new LinkedHashMap<>();
        for (int r = 0; r < rows; r++) {
            String[] cells = rowTokens[r].split(",", -1);
            if (cells.length != cols) throw new BusinessException("Dữ liệu ma trận không khớp số cột. Tải lại trang.");
            for (int c = 1; c <= cols; c++) {
                String cell = cells[c - 1].trim();
                if (cell.isEmpty()) continue; // lối đi
                boolean broken = cell.endsWith("x");
                String idPart = broken ? cell.substring(0, cell.length() - 1) : cell;
                int typeId;
                try {
                    typeId = Integer.parseInt(idPart);
                } catch (NumberFormatException e) {
                    throw new BusinessException("Ô " + rowLabel(r) + c + " có dữ liệu không hợp lệ.");
                }
                if (!typeIds.contains(typeId))
                    throw new BusinessException("Ô " + rowLabel(r) + c + " dùng loại ghế không tồn tại.");
                Seat s = new Seat();
                s.setRoomId(roomId);
                s.setRowNum(r + 1);
                s.setColNum(c);
                s.setRowLabel(rowLabel(r));
                s.setSeatNumber(c);
                s.setSeatTypeId(typeId);
                s.setStatus(broken ? Seat.STATUS_BROKEN : Seat.STATUS_ACTIVE);
                seats.put(s.getCoordKey(), s);
            }
        }
        return seats;
    }
}
