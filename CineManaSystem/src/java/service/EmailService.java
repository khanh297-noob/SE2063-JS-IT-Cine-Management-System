package service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // Gmail dùng để gửi email
    private final String senderEmail = "cinego636@gmail.com";

    // App Password của Gmail
    private final String appPassword = "hucfyadualsmcezd";

    public boolean sendOtpEmail(String receiverEmail, String otpCode) {

        try {
            // Cấu hình SMTP của Gmail
            Properties props = new Properties();

            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // Đăng nhập Gmail
            Session session = Session.getInstance(
                    props,
                    new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            senderEmail,
                            appPassword
                    );
                }
            }
            );

            // Tạo email
            MimeMessage message = new MimeMessage(session);

            // Email người gửi
            message.setFrom(new InternetAddress(senderEmail));

            // Email người nhận
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(receiverEmail)
            );

            // Tiêu đề
            message.setSubject("Mã OTP xác thực tài khoản", "UTF-8");

            // Nội dung
            String content
                    = "Xin chào,\n\n"
                    + "Mã OTP xác thực tài khoản của bạn là: "
                    + otpCode
                    + "\n\n"
                    + "Mã OTP có hiệu lực trong 15 phút."
                    + "\n\n"
                    + "Vui lòng không chia sẻ mã OTP này với người khác.";

            message.setText(content, "UTF-8");

            // Gửi email
            Transport.send(message);

            System.out.println("Đã gửi OTP đến: " + receiverEmail);

            return true;

        } catch (MessagingException e) {

            e.printStackTrace();

            return false;
        }
    }

    public boolean sendPasswordEmail(String receiverEmail, String newPassword) {

        try {
            // Cấu hình SMTP của Gmail
            Properties props = new Properties();

            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // Tạo session đăng nhập Gmail
            Session session = Session.getInstance(
                    props,
                    new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            senderEmail,
                            appPassword
                    );
                }
            }
            );

            // Tạo email
            MimeMessage message = new MimeMessage(session);

            // Người gửi
            message.setFrom(new InternetAddress(senderEmail));

            // Người nhận
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(receiverEmail)
            );

            // Tiêu đề
            message.setSubject("Mật khẩu mới của tài khoản CinGo", "UTF-8");

            // Nội dung
            String content
                    = "Xin chào,\n\n"
                    + "Bạn đã yêu cầu cấp lại mật khẩu tài khoản CinGo.\n\n"
                    + "Mật khẩu mới của bạn là: " + newPassword + "\n\n"
                    + "Vui lòng đăng nhập và đổi mật khẩu ngay sau khi đăng nhập.\n\n"
                    + "Nếu bạn không thực hiện yêu cầu này, vui lòng liên hệ quản trị viên.";

            message.setText(content, "UTF-8");

            // Gửi email
            Transport.send(message);

            System.out.println("Đã gửi mật khẩu đến: " + receiverEmail);

            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

}
