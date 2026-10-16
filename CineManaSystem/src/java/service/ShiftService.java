package service;

import dao.AuditLogDAO;
import dao.BranchDAO;
import dao.PaymentDAO;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.AuditLog;
import model.Branch;
import model.User;
import model.WorkShift;

/**
 * Ca làm việc (Working hours) + két tiền (Check cash balance) của nhân viên quầy / soát vé.
 * Ca được lưu trong AuditLog:  SHIFT_START|branch=1|float=500000  và  SHIFT_END|branch=1|counted=..|expected=..
 */
public class ShiftService {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public List<Branch> getBranches() {
        return branchDAO.getAll();
    }

    // Ca đang mở của nhân viên, chưa vào ca thì trả về null
    public WorkShift getActiveShift(User u) {
        AuditLog last = auditLogDAO.getLastShiftLog(u.getUserId());
        if (last == null || !"SHIFT_START".equals(last.getType())) {
            return null;
        }
        return toShift(last);
    }

    public void startShift(User u, int branchId, BigDecimal openingFloat, String ip) {
        if (getActiveShift(u) != null) {
            throw new IllegalArgumentException("Bạn đang trong một ca làm việc, hãy kết thúc ca trước.");
        }
        if (branchDAO.getById(branchId) == null) {
            throw new IllegalArgumentException("The Branch is required.");
        }
        if (openingFloat == null || openingFloat.signum() < 0) {
            throw new IllegalArgumentException("Invalid Opening cash format.");
        }
        auditLogDAO.insert(u.getUserId(), "SHIFT_START|branch=" + branchId + "|float=" + openingFloat.toPlainString(), ip);
    }

    // Kết ca, trả về thông báo có kèm chênh lệch két
    public String endShift(User u, BigDecimal countedCash, String ip) {
        WorkShift s = getActiveShift(u);
        if (s == null) {
            throw new IllegalArgumentException("Bạn chưa bắt đầu ca làm việc.");
        }
        if (countedCash == null || countedCash.signum() < 0) {
            throw new IllegalArgumentException("The Counted cash is required.");
        }
        BigDecimal expected = (BigDecimal) getCashBalance(u, s).get("expected");
        auditLogDAO.insert(u.getUserId(), "SHIFT_END|branch=" + s.getBranchId()
                + "|counted=" + countedCash.toPlainString() + "|expected=" + expected.toPlainString(), ip);
        BigDecimal diff = countedCash.subtract(expected);
        if (diff.signum() == 0) {
            return "Kết thúc ca thành công. Két tiền khớp.";
        }
        return "Kết thúc ca thành công. Chênh lệch két: " + String.format("%,.0f ₫", diff);
    }

    /**
     * Két tiền của ca: tiền phải có = tiền đầu ca + tiền mặt thu - tiền mặt hoàn.
     * Trả về Map để JSP đọc: ${bal.expected}, ${bal.cashSales}...
     */
    public Map<String, Object> getCashBalance(User u, WorkShift s) {
        Timestamp from = s.getStartTime();
        Timestamp to = new Timestamp(System.currentTimeMillis() + 60_000);
        BigDecimal cashSales = paymentDAO.sumCounterSales(u.getUserId(), "Cash", from, to);
        BigDecimal cardSales = paymentDAO.sumCounterSales(u.getUserId(), "Credit Card", from, to);

        BigDecimal cashRefund = BigDecimal.ZERO;
        BigDecimal cardRefund = BigDecimal.ZERO;
        List<AuditLog> refunds = auditLogDAO.getLogs(u.getUserId(), "COUNTER_REFUND", from, to);
        for (AuditLog log : refunds) {
            BigDecimal amount = new BigDecimal(log.getParam("amount"));
            if ("Cash".equals(log.getParam("method"))) {
                cashRefund = cashRefund.add(amount);
            } else {
                cardRefund = cardRefund.add(amount);
            }
        }

        Map<String, Object> bal = new HashMap<>();
        bal.put("openingFloat", s.getOpeningFloat());
        bal.put("cashSales", cashSales);
        bal.put("cardSales", cardSales);
        bal.put("cashRefund", cashRefund);
        bal.put("cardRefund", cardRefund);
        bal.put("refundCount", refunds.size());
        bal.put("transactionCount", paymentDAO.countCounterSales(u.getUserId(), from, to));
        bal.put("expected", s.getOpeningFloat().add(cashSales).subtract(cashRefund));
        bal.put("netRevenue", cashSales.add(cardSales).subtract(cashRefund).subtract(cardRefund));
        return bal;
    }

    /**
     * Danh sách ca trong khoảng ngày (ghép SHIFT_START với SHIFT_END kế tiếp của cùng nhân viên).
     * userId = 0 nghĩa là tất cả nhân viên.
     */
    public List<WorkShift> getShifts(int userId, LocalDate from, LocalDate to) {
        List<AuditLog> logs = auditLogDAO.getLogs(userId, "SHIFT_",
                Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        List<WorkShift> result = new ArrayList<>();
        Map<Integer, WorkShift> openShifts = new HashMap<>();   // userId -> ca chưa kết thúc
        for (AuditLog log : logs) {
            if ("SHIFT_START".equals(log.getType())) {
                WorkShift s = toShift(log);
                openShifts.put(log.getUserId(), s);
                result.add(s);
            } else {
                WorkShift s = openShifts.remove(log.getUserId());
                if (s != null) {
                    s.setEndTime(log.getTime());
                    s.setCountedCash(new BigDecimal(log.getParam("counted")));
                    s.setExpectedCash(new BigDecimal(log.getParam("expected")));
                }
            }
        }
        // Ca mới nhất lên đầu
        result.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));
        return result;
    }

    // Dựng WorkShift từ log SHIFT_START
    private WorkShift toShift(AuditLog log) {
        WorkShift s = new WorkShift();
        s.setUserId(log.getUserId());
        s.setStaffName(log.getFullName());
        s.setRole(log.getRole());
        s.setStartTime(log.getTime());
        s.setOpeningFloat(new BigDecimal(log.getParam("float")));
        int branchId = Integer.parseInt(log.getParam("branch"));
        s.setBranchId(branchId);
        Branch b = branchDAO.getById(branchId);
        s.setBranchName(b == null ? "" : b.getName());
        return s;
    }
}
