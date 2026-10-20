package cinemanagement.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;
    private String status;
    private Integer branchId;
    private String branchName;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public boolean isCustomer() { return "Customer".equals(role); }
    public boolean isCashier() { return "Ticket Cashier".equals(role); }
    public boolean isChecker() { return "Ticket Checker".equals(role); }
    public boolean isManager() { return "Branch Management".equals(role); }
    public boolean isAdmin() { return "Administrator".equals(role); }
    public boolean isActive() { return "Active".equals(status); }

    public User sessionCopy() {
        User u = new User();
        u.userId = userId; u.fullName = fullName; u.email = email; u.phone = phone;
        u.role = role; u.status = status; u.branchId = branchId; u.branchName = branchName;
        return u;
    }
}
