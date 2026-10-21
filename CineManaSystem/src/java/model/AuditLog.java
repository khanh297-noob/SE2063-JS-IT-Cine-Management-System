package model;

import java.sql.Timestamp;

/**
 * Một dòng nhật ký (bảng AuditLog).
 * Cột Action có dạng "LOAI|key1=value1|key2=value2", ví dụ:
 *   SHIFT_START|branch=1|float=500000
 *   SHIFT_END|branch=1|counted=1250000|expected=1250000
 *   COUNTER_REFUND|booking=12|amount=240000|method=Cash
 */
public class AuditLog {

    private int logId;
    private int userId;
    private String fullName;
    private String role;
    private String action;
    private Timestamp time;

    public AuditLog() {
    }

    // Phần trước dấu | (vd "SHIFT_START")
    public String getType() {
        return action.split("\\|")[0];
    }

    // Lấy giá trị theo key, vd getParam("branch") -> "1". Không có thì trả về null.
    public String getParam(String key) {
        String[] parts = action.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith(key + "=")) {
                return parts[i].substring(key.length() + 1);
            }
        }
        return null;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }
}
