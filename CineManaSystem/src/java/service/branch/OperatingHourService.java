package service.branch;

import dao.branch.BranchDAO;
import dao.branch.BranchOperatingHourDAO;
import model.branch.BranchOperatingHour;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manage Operating Hours.
 *
 * BR-OH-01  Lưu đủ 7 ngày; mỗi ngày hoặc nghỉ, hoặc có giờ mở và giờ đóng khác nhau.
 * BR-OH-02  Giờ đóng nhỏ hơn giờ mở = đóng cửa qua đêm, tối đa đến 06:00 sáng hôm sau.
 * BR-OH-03  Mỗi tuần phải có ít nhất một ngày mở cửa.
 */
public class OperatingHourService {

    private static final LocalTime LATEST_OVERNIGHT_CLOSE = LocalTime.of(6, 0);

    private final BranchDAO branchDAO = new BranchDAO();
    private final BranchOperatingHourDAO hourDAO = new BranchOperatingHourDAO();

    /** Lịch 7 ngày; ngày nào chưa lưu thì điền mặc định 09:00-23:00. */
    public List<BranchOperatingHour> getWeek(int branchId) throws SQLException, BusinessException {
        List<BranchOperatingHour> saved = JdbcContext.query(con -> {
            if (branchDAO.findById(con, branchId) == null)
                throw new NotFoundException("Không tìm thấy chi nhánh #" + branchId);
            return hourDAO.findByBranch(con, branchId);
        });
        Map<Integer, BranchOperatingHour> byDay = new HashMap<>();
        for (BranchOperatingHour h : saved) byDay.put(h.getDayOfWeek(), h);

        List<BranchOperatingHour> week = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            BranchOperatingHour h = byDay.get(d);
            if (h == null) {
                h = new BranchOperatingHour(branchId, d);
                h.setOpenTime(LocalTime.of(9, 0));
                h.setCloseTime(LocalTime.of(23, 0));
            }
            week.add(h);
        }
        return week;
    }

    public void save(int branchId, List<BranchOperatingHour> week) throws SQLException, BusinessException {
        List<String> errors = new ArrayList<>();
        if (week.size() != 7) errors.add("Cần nhập đủ 7 ngày trong tuần.");

        boolean anyOpen = false;
        for (BranchOperatingHour h : week) {
            h.setBranchId(branchId);
            if (h.isClosed()) continue;
            anyOpen = true;
            String day = h.getDayName();
            if (h.getOpenTime() == null || h.getCloseTime() == null) {
                errors.add(day + ": nhập giờ mở và giờ đóng, hoặc đánh dấu nghỉ.");
            } else if (h.getOpenTime().equals(h.getCloseTime())) {
                errors.add(day + ": giờ mở và giờ đóng không được trùng nhau.");
            } else if (h.isOvernight() && h.getCloseTime().isAfter(LATEST_OVERNIGHT_CLOSE)) {
                errors.add(day + ": đóng cửa qua đêm chỉ đến tối đa 06:00 sáng hôm sau.");
            }
        }
        if (!anyOpen) errors.add("Chi nhánh phải mở cửa ít nhất một ngày trong tuần.");
        if (!errors.isEmpty()) throw new BusinessException(errors);

        JdbcContext.transaction(con -> {
            if (branchDAO.findById(con, branchId) == null)
                throw new NotFoundException("Không tìm thấy chi nhánh #" + branchId);
            hourDAO.replaceForBranch(con, branchId, week);
            return null;
        });
    }
}
