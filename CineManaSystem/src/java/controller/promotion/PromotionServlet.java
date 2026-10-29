package controller.promotion;

import service.common.AccessService;
import service.f_b.F_BItemService;
import service.f_b.F_BStockService;
import service.promotion.PricingService;
import service.promotion.PromotionService;
import util.FormUtil;
import util.SessionUtil;
import model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

@WebServlet(urlPatterns = {
        "/staff/promotions/manage", "/staff/promotions/discounts", "/staff/promotions/limits",
        "/staff/promotions/ticket-prices", "/staff/promotions/fnb-prices", "/staff/promotions/validate"
})
public class PromotionServlet extends HttpServlet {
    private final PromotionService service = new PromotionService();
    private final PricingService pricing = new PricingService();
    private final F_BStockService stocks = new F_BStockService();
    private final F_BItemService items = new F_BItemService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User actor = SessionUtil.getLoggedInUser(request);
            AccessService.promotion(actor);
            String path = request.getServletPath();
            String page;
            if (path.endsWith("/validate")) {
                page = "validate";
            } else if (path.endsWith("/ticket-prices") || path.endsWith("/fnb-prices")) {
                pricing.requireConfiguration(actor);
                request.setAttribute("branchList", stocks.allowedBranches(actor));
                Integer branch = optionalInt(request, "branchId");
                if (branch != null) {
                    request.setAttribute("selectedBranchId", branch);
                    if (path.endsWith("/ticket-prices"))
                        request.setAttribute("showtimes", pricing.showtimes(actor, branch));
                    else request.setAttribute("stockList", stocks.list(actor, branch));
                } else if (path.endsWith("/fnb-prices") && "Administrator".equals(actor.getRole())) {
                    request.setAttribute("itemList", items.getAllItems(actor));
                }
                page = path.endsWith("/ticket-prices") ? "ticket-prices" : "f_b-prices";
            } else {
                request.setAttribute("promotionList", service.list(actor));
                request.setAttribute("branchList", stocks.allowedBranches(actor));
                Integer edit = optionalInt(request, "edit");
                if (edit != null) request.setAttribute("editPromotion", service.editable(actor, edit));
                page = path.endsWith("/discounts") ? "discount" : path.endsWith("/limits") ? "limits" : "manage";
            }
            FormUtil.csrf(request);
            request.getRequestDispatcher("/WEB-INF/view/promotion/" + page + ".jsp").forward(request, response);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
            response.sendRedirect(request.getContextPath() + request.getServletPath());
        } catch (SQLException e) {
            getServletContext().log("Không đọc được khuyến mãi/giá", e);
            response.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer branch = null;
        try {
            FormUtil.checkCsrf(request);
            User actor = SessionUtil.getLoggedInUser(request);
            String action = request.getParameter("action");
            switch (action == null ? "" : action) {
                case "save":
                    Promotion p = new Promotion();
                    p.setPromoId(FormUtil.integer(request, "promoId"));
                    p.setDiscountAmount(FormUtil.money(request, "discountAmount"));
                    p.setMinSpend(FormUtil.money(request, "minSpend"));
                    p.setUsageLimit(FormUtil.integer(request, "usageLimit"));
                    p.setStartDate(java.sql.Date.valueOf(request.getParameter("startDate")));
                    p.setEndDate(java.sql.Date.valueOf(request.getParameter("endDate")));
                    String scope = request.getParameter("scope");
                    if (!Set.of("ALL", "SELECTED").contains(scope == null ? "" : scope))
                        throw new IllegalArgumentException("Phạm vi không hợp lệ.");
                    Set<Integer> selected = new TreeSet<>();
                    String[] values = request.getParameterValues("branchIds");
                    if (values != null) for (String value : values) selected.add(Integer.parseInt(value));
                    int id = service.save(actor, p, "ALL".equals(scope), selected);
                    request.getSession().setAttribute("toastSuccess", "Đã lưu voucher #" + id);
                    break;
                case "discount":
                    service.discount(actor, FormUtil.integer(request, "promoId"), FormUtil.money(request, "discountAmount"));
                    break;
                case "limit":
                    service.limit(actor, FormUtil.integer(request, "promoId"), FormUtil.integer(request, "usageLimit"));
                    break;
                case "delete":
                    service.delete(actor, FormUtil.integer(request, "promoId"));
                    break;
                case "ticket-price":
                    branch = optionalInt(request, "branchId");
                    pricing.ticketPrice(actor, FormUtil.integer(request, "showtimeId"), FormUtil.money(request, "basePrice"));
                    break;
                case "fnb-price":
                    branch = optionalInt(request, "branchId");
                    String rawPrice = request.getParameter("price");
                    BigDecimal price = rawPrice == null || rawPrice.isBlank() ? null : FormUtil.money(request, "price");
                    pricing.fnbPrice(actor, FormUtil.integer(request, "itemId"), branch, price);
                    break;
                case "preview":
                case "apply":
                    BigDecimal discount = service.check(actor, FormUtil.integer(request, "bookingId"),
                            request.getParameter("promoCode"), "apply".equals(action));
                    request.getSession().setAttribute("toastSuccess", "Voucher hợp lệ, giảm " + discount + " đ.");
                    break;
                case "remove":
                    service.remove(actor, FormUtil.integer(request, "bookingId"));
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            if (request.getSession().getAttribute("toastSuccess") == null)
                request.getSession().setAttribute("toastSuccess", "Đã cập nhật.");
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Không cập nhật được khuyến mãi/giá", e);
            request.getSession().setAttribute("toastError", "Không lưu được dữ liệu.");
        }
        response.sendRedirect(request.getContextPath() + request.getServletPath()
                + (branch == null ? "" : "?branchId=" + branch));
    }

    private Integer optionalInt(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        return raw == null || raw.isBlank() ? null : FormUtil.integer(request, name);
    }
}
