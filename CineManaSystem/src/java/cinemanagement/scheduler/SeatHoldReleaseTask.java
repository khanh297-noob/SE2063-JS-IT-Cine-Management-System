package cinemanagement.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cinemanagement.dao.SeatHoldDAO;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class SeatHoldReleaseTask implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(SeatHoldReleaseTask.class.getName());
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "seat-hold-release");
            t.setDaemon(true);
            return t;
        });
        SeatHoldDAO dao = new SeatHoldDAO();
        executor.scheduleWithFixedDelay(() -> {
            try {
                int n = dao.deleteExpired();
                if (n > 0) {
                    LOG.info("JOB-01: released " + n + " expired seat hold(s)");
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "JOB-01 failed", e);
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
