package scheduler;

import service.PaymentService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Job chạy nền "Payment Status Synchronization": cứ mỗi 1 phút
 * tìm các đơn Pending đã hết 10 phút giữ ghế -> hủy đơn, Payment -> Failed, nhả ghế.
 *
 * @WebListener: Tomcat tự gọi contextInitialized() khi web khởi động
 *               và contextDestroyed() khi web dừng.
 */
@WebListener
public class PaymentSyncJob implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        PaymentService paymentService = new PaymentService();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Chạy lần đầu sau 1 phút, sau đó lặp lại mỗi 1 phút
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int count = paymentService.releaseExpiredBookings();
                if (count > 0) {
                    System.out.println("PaymentSyncJob: đã hủy " + count + " đơn hết giờ giữ ghế");
                }
            } catch (Exception e) {
                // Bắt lỗi để job không bị dừng hẳn, phút sau chạy lại
                System.out.println("PaymentSyncJob lỗi: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
        System.out.println("PaymentSyncJob: đã khởi động (chạy mỗi 1 phút)");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Dừng job khi tắt web, tránh luồng chạy ngầm còn sót lại
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
