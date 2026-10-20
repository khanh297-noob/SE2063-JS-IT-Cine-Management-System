package thirdparty;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Gửi email qua Gmail (SMTP).
 */
public class EmailService {

    // Gmail dùng để gửi email
    private final String senderEmail = "cinego636@gmail.com";

    // App Password của Gmail
    private final String appPassword = "hucfyadualsmcezd";

    /** Gửi mã OTP xác thực tài khoản. Trả về true nếu gửi được. */
    public boolean sendOtpEmail(String receiverEmail, String otpCode) {
        String subject = "Mã OTP xác thực tài khoản";
        String content = "Xin chào,\n\n"
                + "Mã OTP xác thực tài khoản của bạn là: " + otpCode + "\n\n"
                + "Mã OTP có hiệu lực trong 15 phút.\n\n"
                + "Vui lòng không chia sẻ mã OTP này với người khác.";
        return sendEmail(receiverEmail, subject, content);
    }

    /** Gửi mật khẩu mới (dùng cho chức năng quên mật khẩu). Trả về true nếu gửi được. */
    public boolean sendPasswordEmail(String receiverEmail, String newPassword) {
        String subject = "Mật khẩu mới của tài khoản CinGo";
        String content = "Xin chào,\n\n"
                + "Bạn đã yêu cầu cấp lại mật khẩu tài khoản CinGo.\n\n"
                + "Mật khẩu mới của bạn là: " + newPassword + "\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay sau khi đăng nhập.\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng liên hệ quản trị viên.";
        return sendEmail(receiverEmail, subject, content);
    }

    /** Hàm gửi email dùng chung cho các loại email ở trên. */
    private boolean sendEmail(String receiverEmail, String subject, String content) {
        try {
            // Bước 1: cấu hình SMTP của Gmail
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // Bước 2: đăng nhập Gmail
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, appPassword);
                }
            });

            // Bước 3: soạn email
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiverEmail));
            message.setSubject(subject, "UTF-8");
            message.setText(content, "UTF-8");

            // Bước 4: gửi
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
