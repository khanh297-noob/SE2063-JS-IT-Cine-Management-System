package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import service.movie.MovieStatusService;

/**
 * Đối soát trạng thái phim BR-MS-03 (MovieStatusService.enforceStatusRules):
 * - Khi ứng dụng khởi động: dữ liệu có thể bị sửa thẳng trong database lúc ứng dụng tắt (vd xoá chi nhánh).
 * - Mỗi ngày lúc 00:01: khung ngày chiếu của chi nhánh có thể vừa hết hạn (phim không còn chi nhánh đang chiếu).
 * Lỗi kết nối DB chỉ ghi log, không chặn ứng dụng; trang quản lý phim sẽ đối soát lại khi mở.
 */
@WebListener
public class MovieRuleStartupCheck implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(MovieRuleStartupCheck.class.getName());
    private static final LocalTime DAILY_RUN = LocalTime.of(0, 1);

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        runCheck("khởi động");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "movie-status-daily-check");
            t.setDaemon(true);
            return t;
        });
        LocalDateTime next = LocalDate.now().plusDays(1).atTime(DAILY_RUN);
        long delay = Duration.between(LocalDateTime.now(), next).toMillis();
        scheduler.scheduleAtFixedRate(() -> runCheck("hằng ngày"), delay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void runCheck(String when) {
        try {
            MovieStatusService.RuleCheckResult r = new MovieStatusService().enforceStatusRules();
            for (String s : r.getDemoted()) LOG.warning("BR-MS-03 (" + when + "): chuyển về Coming Soon " + s);
            for (String s : r.getEnded()) LOG.warning("BR-MS-03 (" + when + "): chuyển sang End of Screening " + s);
            for (String s : r.getReadyToShow()) LOG.info("BR-MS-04 (" + when + "): có thể chuyển sang Now Showing " + s);
            for (String s : r.getNeedsAttention()) LOG.warning("BR-MS-03 (" + when + "): cần Admin xử lý " + s);
            if (r.isClean()) LOG.info("BR-MS-03 (" + when + "): không có phim nào cần đổi trạng thái.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Không đối soát được trạng thái phim (BR-MS-03, " + when + ")", e);
        }
    }
}
