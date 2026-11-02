package controller.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.User;
import model.notification.Notification;
import service.notification.NotificationService;
import util.SessionUtil;

/**
 * API JSON cho chuông thông báo (notification-bell.js). Cần đăng nhập: AuthFilter chặn khách chưa đăng nhập.
 * GET  /notifications/count                       {"unread": n}
 * GET  /notifications/list?limit=10[&before=id]   {"unread": n, "hasMore": bool, "items": [...]}
 * POST /notifications/read      id                {"ok": true, "unread": n}
 * POST /notifications/read-all                    {"ok": true, "unread": 0}
 * Lỗi database (vd chưa tạo bảng Notification): trả dữ liệu rỗng kèm "available": false, không lỗi 500.
 */
@WebServlet(name = "notifications", urlPatterns = {"/notifications/*"})
public class NotificationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(NotificationServlet.class.getName());
    private final NotificationService service = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req, resp);
        if (user == null) return;
        String action = action(req);
        JsonObject out = new JsonObject();
        try {
            switch (action) {
                case "count" -> out.addProperty("unread", service.countUnread(user.getUserID()));
                case "list" -> {
                    int limit = intParam(req, "limit", NotificationService.PAGE_SIZE);
                    Integer before = req.getParameter("before") == null ? null : intParam(req, "before", 0);
                    NotificationService.Page page = service.list(user.getUserID(), before, limit);
                    JsonArray items = new JsonArray();
                    for (Notification n : page.getItems()) items.add(toJson(n));
                    out.addProperty("unread", page.getUnread());
                    out.addProperty("hasMore", page.isHasMore());
                    out.add("items", items);
                }
                default -> { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            }
            out.addProperty("available", true);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không đọc được thông báo của user #" + user.getUserID(), e);
            out = new JsonObject();
            out.addProperty("unread", 0);
            out.addProperty("hasMore", false);
            out.add("items", new JsonArray());
            out.addProperty("available", false);
        }
        write(resp, out);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req, resp);
        if (user == null) return;
        JsonObject out = new JsonObject();
        try {
            switch (action(req)) {
                case "read" -> {
                    int id = intParam(req, "id", -1);
                    if (id <= 0) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
                    service.markRead(user.getUserID(), id);
                }
                case "read-all" -> service.markAllRead(user.getUserID());
                default -> { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            }
            out.addProperty("ok", true);
            out.addProperty("unread", service.countUnread(user.getUserID()));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không cập nhật được thông báo của user #" + user.getUserID(), e);
            out.addProperty("ok", false);
        }
        write(resp, out);
    }

    private static User currentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = SessionUtil.getLoggedInUser(req);
        if (user == null) resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return user;
    }

    private static String action(HttpServletRequest req) {
        String p = req.getPathInfo();
        return p == null ? "" : p.replaceFirst("^/", "");
    }

    private static int intParam(HttpServletRequest req, String name, int def) {
        try {
            String v = req.getParameter(name);
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static JsonObject toJson(Notification n) {
        JsonObject o = new JsonObject();
        o.addProperty("id", n.getNotificationId());
        o.addProperty("type", n.getType());
        o.addProperty("title", n.getTitle());
        o.addProperty("content", n.getContent());
        o.addProperty("link", n.getLink());
        o.addProperty("read", n.isRead());
        o.addProperty("createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().withNano(0).toString());
        return o;
    }

    private static void write(HttpServletResponse resp, JsonObject out) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(out.toString());
    }
}
