package service.movie;

import dao.ShowtimeDAO;
import dao.branch.BranchDAO;
import dao.movie.MovieBranchDAO;
import dao.movie.MovieDAO;
import model.branch.Branch;
import model.movie.BranchAvailabilityDTO;
import model.movie.Movie;
import model.movie.MovieBranch;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configure Available Branches.
 *
 * BR-AB-01  Phim "End of Screening" chỉ xem, không cấu hình.
 * BR-AB-02  Không thêm phim vào chi nhánh đang ngưng hoạt động (chi nhánh đã có thì giữ/gỡ được).
 * BR-AB-03  Không gỡ chi nhánh khi phim còn suất chiếu sắp tới tại đó.
 * BR-AB-04  Từ ngày <= đến ngày; khoảng ngày không được loại bỏ suất chiếu đã xếp.
 * BR-AB-05  Khi thêm mới hoặc đổi ngày, ngày kết thúc không được ở quá khứ.
 * BR-AB-06  Phim "Now Showing" phải còn ít nhất 1 chi nhánh đang hoạt động có khung ngày chiếu gồm hôm nay
 *           (BR-MS-01): không gỡ hết, không đẩy hết khung ngày chiếu ra khỏi hôm nay.
 */
public class MovieBranchService {

    private final MovieDAO movieDAO = new MovieDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final MovieBranchDAO movieBranchDAO = new MovieBranchDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    /** Dữ liệu form gửi lên cho một chi nhánh. */
    public static final class BranchSelection {
        private final int branchId;
        private final boolean selected;
        private final String fromRaw;
        private final String toRaw;

        public BranchSelection(int branchId, boolean selected, String fromRaw, String toRaw) {
            this.branchId = branchId;
            this.selected = selected;
            this.fromRaw = fromRaw;
            this.toRaw = toRaw;
        }

        public int getBranchId() { return branchId; }
        public boolean isSelected() { return selected; }
        public String getFromRaw() { return fromRaw; }
        public String getToRaw() { return toRaw; }
    }

    public static final class SaveResult {
        private final int added;
        private final int removed;
        private final int updated;

        SaveResult(int added, int removed, int updated) {
            this.added = added;
            this.removed = removed;
            this.updated = updated;
        }

        public int getAdded() { return added; }
        public int getRemoved() { return removed; }
        public int getUpdated() { return updated; }
        public boolean isEmpty() { return added + removed + updated == 0; }
    }

    public List<BranchAvailabilityDTO> getAvailability(int movieId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> load(con, movieId));
    }

    /** Khi lưu lỗi: dữ liệu DB phủ thêm lựa chọn người dùng vừa gửi, để form hiển thị lại. */
    public List<BranchAvailabilityDTO> previewSubmission(int movieId, List<BranchSelection> selections)
            throws SQLException, BusinessException {
        Map<Integer, BranchSelection> byId = index(selections);
        List<BranchAvailabilityDTO> rows = new ArrayList<>();
        for (BranchAvailabilityDTO cur : getAvailability(movieId)) {
            BranchAvailabilityDTO row = cur.copy();
            BranchSelection sel = byId.get(row.getBranch().getBranchId());
            if (sel != null) {
                row.setAssigned(sel.isSelected() || row.isLocked());
                row.setAvailableFrom(parseQuietly(sel.getFromRaw()));
                row.setAvailableTo(parseQuietly(sel.getToRaw()));
            }
            rows.add(row);
        }
        return rows;
    }

    public SaveResult save(int movieId, List<BranchSelection> selections) throws SQLException, BusinessException {
        return JdbcContext.transaction(con -> {
            Movie movie = movieDAO.findById(con, movieId);
            if (movie == null) throw new NotFoundException("Không tìm thấy phim #" + movieId);
            if (movie.isEnded())
                throw new BusinessException("Phim \"" + movie.getTitle() + "\" đã ngưng chiếu nên không thể đổi chi nhánh.");

            Map<Integer, BranchSelection> byId = index(selections);
            LocalDate today = LocalDate.now();
            List<String> errors = new ArrayList<>();
            List<MovieBranch> toInsert = new ArrayList<>();
            List<MovieBranch> toUpdate = new ArrayList<>();
            List<Integer> toRemove = new ArrayList<>();
            List<BranchAvailabilityDTO> current = load(con, movieId);

            for (BranchAvailabilityDTO cur : current) {
                Branch branch = cur.getBranch();
                BranchSelection sel = byId.get(branch.getBranchId());
                if (sel == null) continue; // không có trên form -> giữ nguyên
                String name = branch.getName();

                if (!sel.isSelected()) {
                    if (cur.isPersisted()) {
                        if (cur.getUpcomingShowtimes() > 0)
                            errors.add("Không thể gỡ " + name + ": còn " + cur.getUpcomingShowtimes()
                                    + " suất chiếu sắp tới.");
                        else toRemove.add(branch.getBranchId());
                    }
                    continue;
                }
                if (!cur.isPersisted() && !branch.isActive()) {
                    errors.add(name + " đang ngưng hoạt động nên không thể thêm phim.");
                    continue;
                }

                LocalDate from;
                LocalDate to;
                try {
                    from = parse(sel.getFromRaw());
                    to = parse(sel.getToRaw());
                } catch (DateTimeParseException e) {
                    errors.add(name + ": ngày không hợp lệ.");
                    continue;
                }
                if (from != null && to != null && to.isBefore(from)) {
                    errors.add(name + ": ngày kết thúc phải sau hoặc trùng ngày bắt đầu.");
                    continue;
                }
                boolean changed = !Objects.equals(from, cur.getAvailableFrom()) || !Objects.equals(to, cur.getAvailableTo());
                if (to != null && to.isBefore(today) && (!cur.isPersisted() || changed)) {
                    errors.add(name + ": ngày kết thúc đã qua.");
                    continue;
                }
                if (cur.isPersisted() && !changed) continue;

                if (from != null || to != null) {
                    int outside = showtimeDAO.countUpcomingAtBranch(con, movieId, branch.getBranchId(), from, to);
                    if (outside > 0) {
                        errors.add(name + ": có " + outside + " suất chiếu nằm ngoài khoảng ngày mới.");
                        continue;
                    }
                }
                MovieBranch mb = new MovieBranch();
                mb.setMovieId(movieId);
                mb.setBranchId(branch.getBranchId());
                mb.setAvailableFrom(from);
                mb.setAvailableTo(to);
                if (cur.isPersisted()) toUpdate.add(mb); else toInsert.add(mb);
            }
            if (!errors.isEmpty()) throw new BusinessException(errors);

            // BR-AB-06: sau khi lưu, phim đang chiếu phải còn ít nhất 1 chi nhánh đang chiếu hôm nay (BR-MS-01)
            if (movie.isNowShowing() && !screeningAfterSave(current, toInsert, toUpdate, toRemove, today))
                throw new BusinessException("Phim \"" + movie.getTitle() + "\" đang chiếu nên phải còn ít nhất 1 chi nhánh "
                        + "đang hoạt động có khung ngày chiếu gồm hôm nay. Muốn ngừng chiếu, hãy chuyển phim sang End of Screening.");

            for (Integer branchId : toRemove) movieBranchDAO.delete(con, movieId, branchId);
            for (MovieBranch mb : toInsert) movieBranchDAO.insert(con, mb);
            for (MovieBranch mb : toUpdate) movieBranchDAO.updateWindow(con, mb);
            return new SaveResult(toInsert.size(), toRemove.size(), toUpdate.size());
        });
    }

    /** Sau khi áp dụng thêm / sửa / gỡ, còn chi nhánh nào đang hoạt động và có khung ngày chiếu gồm today không. */
    private static boolean screeningAfterSave(List<BranchAvailabilityDTO> current, List<MovieBranch> toInsert,
                                              List<MovieBranch> toUpdate, List<Integer> toRemove, LocalDate today) {
        Map<Integer, MovieBranch> changed = new HashMap<>();
        for (MovieBranch mb : toInsert) changed.put(mb.getBranchId(), mb);
        for (MovieBranch mb : toUpdate) changed.put(mb.getBranchId(), mb);
        for (BranchAvailabilityDTO cur : current) {
            int branchId = cur.getBranch().getBranchId();
            MovieBranch mb = changed.get(branchId);
            boolean assigned = mb != null || (cur.isPersisted() && !toRemove.contains(branchId));
            if (!assigned || !cur.getBranch().isActive()) continue;
            LocalDate from = mb != null ? mb.getAvailableFrom() : cur.getAvailableFrom();
            LocalDate to = mb != null ? mb.getAvailableTo() : cur.getAvailableTo();
            if ((from == null || !from.isAfter(today)) && (to == null || !to.isBefore(today))) return true;
        }
        return false;
    }

    /** Cho module Showtime gọi trước khi tạo suất chiếu. */
    public boolean isMovieAvailableAt(int movieId, int branchId, LocalDate date) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Movie m = movieDAO.findById(con, movieId);
            return m != null && !m.isEnded() && movieBranchDAO.isAvailable(con, movieId, branchId, date);
        });
    }

    // ------------------------------------------------------------------

    private List<BranchAvailabilityDTO> load(Connection con, int movieId) throws SQLException {
        Map<Integer, MovieBranch> assigned = new HashMap<>();
        for (MovieBranch mb : movieBranchDAO.findByMovie(con, movieId)) assigned.put(mb.getBranchId(), mb);
        Map<Integer, Integer> upcoming = showtimeDAO.countUpcomingByMovieGroupedByBranch(con, movieId);

        List<BranchAvailabilityDTO> rows = new ArrayList<>();
        for (Branch b : branchDAO.findAll(con, null)) {
            MovieBranch mb = assigned.get(b.getBranchId());
            BranchAvailabilityDTO row = new BranchAvailabilityDTO();
            row.setBranch(b);
            row.setPersisted(mb != null);
            row.setAssigned(mb != null);
            row.setAvailableFrom(mb == null ? null : mb.getAvailableFrom());
            row.setAvailableTo(mb == null ? null : mb.getAvailableTo());
            row.setUpcomingShowtimes(upcoming.getOrDefault(b.getBranchId(), 0));
            rows.add(row);
        }
        return rows;
    }

    private static Map<Integer, BranchSelection> index(List<BranchSelection> list) {
        Map<Integer, BranchSelection> map = new HashMap<>();
        if (list != null) for (BranchSelection s : list) map.put(s.getBranchId(), s);
        return map;
    }

    private static LocalDate parse(String raw) {
        return raw == null || raw.isBlank() ? null : LocalDate.parse(raw.trim());
    }

    private static LocalDate parseQuietly(String raw) {
        try {
            return parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
