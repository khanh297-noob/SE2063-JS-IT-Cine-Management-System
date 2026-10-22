package model;

import java.sql.Timestamp;

public class AuditLog {
    private int logId;
    private Integer userId;
    private String userName;
    private String action;
    private Timestamp timestamp;
    private String ipAddress;

    public AuditLog() {
    }

    public AuditLog(int logId, Integer userId, String userName, String action, Timestamp timestamp, String ipAddress) {
        this.logId = logId;
        this.userId = userId;
        this.userName = userName;
        this.action = action;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}