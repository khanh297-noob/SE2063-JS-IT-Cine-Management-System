package model.notification;

import java.time.LocalDateTime;

/** Một thông báo trong web của một người dùng (bảng Notification). */
public class Notification {

    public static final String TYPE_MOVIE = "MOVIE";               // phim mới đang chiếu (gửi khách hàng)
    public static final String TYPE_MOVIE_STATUS = "MOVIE_STATUS"; // phim đổi trạng thái (gửi Admin / Quản lý chi nhánh)
    public static final String TYPE_MOVIE_BRANCH = "MOVIE_BRANCH"; // phim được thêm / gỡ / đổi ngày chiếu ở chi nhánh (gửi Quản lý)
    public static final String TYPE_BRANCH = "BRANCH";             // chi nhánh / phòng / giờ mở cửa / sơ đồ ghế bị sửa (gửi Quản lý)
    public static final String TYPE_BOOKING = "BOOKING";           // đặt vé: giữ ghế chờ thanh toán, huỷ đơn (gửi khách)
    public static final String TYPE_PAYMENT = "PAYMENT";           // thanh toán thành công / thất bại (gửi khách)

    private int notificationId;
    private int userId;
    private String type;
    private String title;
    private String content;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
