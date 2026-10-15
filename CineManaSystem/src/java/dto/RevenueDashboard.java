package dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Dữ liệu cho trang Revenue Dashboard (doanh thu vé + F&B). */
public class RevenueDashboard {

    private String fromDate;
    private String toDate;
    private String scopeLabel;
    private List<String> weekLabels = new ArrayList<>();
    private List<BigDecimal> weeklyTicketRevenue = new ArrayList<>();
    private List<BigDecimal> weeklyFnbRevenue = new ArrayList<>();
    private List<BreakdownTable> tables = new ArrayList<>();

    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }

    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }

    public String getScopeLabel() { return scopeLabel; }
    public void setScopeLabel(String scopeLabel) { this.scopeLabel = scopeLabel; }

    public List<String> getWeekLabels() { return weekLabels; }
    public void setWeekLabels(List<String> weekLabels) { this.weekLabels = weekLabels; }

    public List<BigDecimal> getWeeklyTicketRevenue() { return weeklyTicketRevenue; }
    public void setWeeklyTicketRevenue(List<BigDecimal> v) { this.weeklyTicketRevenue = v; }

    public List<BigDecimal> getWeeklyFnbRevenue() { return weeklyFnbRevenue; }
    public void setWeeklyFnbRevenue(List<BigDecimal> v) { this.weeklyFnbRevenue = v; }

    public List<BreakdownTable> getTables() { return tables; }
    public void setTables(List<BreakdownTable> tables) { this.tables = tables; }
}
