package dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Một bảng phân rã (một tab) trong Revenue Dashboard. */
public class BreakdownTable {

    private final String key;
    private final String title;
    private final String nameHeader;
    private final String totalHeader;
    private final List<RevenueBreakdownRow> rows;

    public BreakdownTable(String key, String title, String nameHeader, String totalHeader,
                          List<RevenueBreakdownRow> rows) {
        this.key = key;
        this.title = title;
        this.nameHeader = nameHeader;
        this.totalHeader = totalHeader;
        this.rows = rows != null ? rows : new ArrayList<RevenueBreakdownRow>();

        BigDecimal grand = BigDecimal.ZERO;
        for (RevenueBreakdownRow r : this.rows) {
            grand = grand.add(r.getTotalRevenue());
        }
        for (RevenueBreakdownRow r : this.rows) {
            r.setShare(grand.signum() > 0
                    ? r.getTotalRevenue().multiply(BigDecimal.valueOf(100)).doubleValue() / grand.doubleValue()
                    : 0);
        }
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getNameHeader() { return nameHeader; }
    public String getTotalHeader() { return totalHeader; }
    public List<RevenueBreakdownRow> getRows() { return rows; }

    public int getTotalTickets() {
        int n = 0;
        for (RevenueBreakdownRow r : rows) n += r.getTickets();
        return n;
    }

    public BigDecimal getTotalTicketRevenue() {
        BigDecimal s = BigDecimal.ZERO;
        for (RevenueBreakdownRow r : rows) s = s.add(r.getTicketRevenue());
        return s;
    }

    public BigDecimal getTotalFnbRevenue() {
        BigDecimal s = BigDecimal.ZERO;
        for (RevenueBreakdownRow r : rows) s = s.add(r.getFnbRevenue());
        return s;
    }

    public BigDecimal getGrandTotal() {
        return getTotalTicketRevenue().add(getTotalFnbRevenue());
    }
}
