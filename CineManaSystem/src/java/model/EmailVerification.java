package model;

import java.sql.Timestamp;

/**
 * Entity ứng với bảng EmailVerification (lưu mã OTP xác thực email).
 */
public class EmailVerification {
    private int verificationID;
    private String email;
    private String otpCode;
    private Timestamp expiryTime;
    private boolean used;

    public EmailVerification() {
    }

    public EmailVerification(int verificationID, String email, String otpCode,
                             Timestamp expiryTime, boolean used) {
        this.verificationID = verificationID;
        this.email = email;
        this.otpCode = otpCode;
        this.expiryTime = expiryTime;
        this.used = used;
    }

    public int getVerificationID() {
        return verificationID;
    }

    public void setVerificationID(int verificationID) {
        this.verificationID = verificationID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public Timestamp getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Timestamp expiryTime) {
        this.expiryTime = expiryTime;
    }

    /** Ứng với cột IsUsed (0 = chưa dùng, 1 = đã dùng). */
    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /** OTP đã quá hạn so với thời điểm hiện tại. */
    public boolean isExpired() {
        return expiryTime == null || expiryTime.before(new Timestamp(System.currentTimeMillis()));
    }

    /** OTP còn dùng được: chưa dùng và chưa hết hạn. */
    public boolean isValid() {
        return !used && !isExpired();
    }
}
