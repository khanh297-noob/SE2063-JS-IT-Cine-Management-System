package dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Toàn bộ dữ liệu hiển thị của trang Báo Cáo Rạp (Branch Report Dashboard). */
public class BranchReportView {

    private String type;
    private String typeLabel;
    private String fromDate;
    private String toDate;
    private String scopeLabel;
    private String branchPill;
    private String dataTag;

    private String chartTitle;
    private String chartUnit;
    private boolean chartStacked;
    private boolean chartPercent;
    private List<String> chartLabels = new ArrayList<>();
    private List<Map<String, Object>> chartDatasets = new ArrayList<>();

    private String overviewTitle = "Tổng Quan Chỉ Số Báo Cáo";
    /** Mỗi phần tử: {nhãn, giá trị, css: "" | "strong" | "total"}. */
    private List<String[]> overview = new ArrayList<>();

    private String sideTitle;
    /** Mỗi phần tử: {tên, giá trị bên phải}. */
    private List<String[]> sideItems = new ArrayList<>();

    private List<ReportTable> tables = new ArrayList<>();
    private String note;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeLabel() { return typeLabel; }
    public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getScopeLabel() { return scopeLabel; }
    public void setScopeLabel(String scopeLabel) { this.scopeLabel = scopeLabel; }
    public String getBranchPill() { return branchPill; }
    public void setBranchPill(String branchPill) { this.branchPill = branchPill; }
    public String getDataTag() { return dataTag; }
    public void setDataTag(String dataTag) { this.dataTag = dataTag; }

    public String getChartTitle() { return chartTitle; }
    public void setChartTitle(String chartTitle) { this.chartTitle = chartTitle; }
    public String getChartUnit() { return chartUnit; }
    public void setChartUnit(String chartUnit) { this.chartUnit = chartUnit; }
    public boolean isChartStacked() { return chartStacked; }
    public void setChartStacked(boolean chartStacked) { this.chartStacked = chartStacked; }
    public boolean isChartPercent() { return chartPercent; }
    public void setChartPercent(boolean chartPercent) { this.chartPercent = chartPercent; }
    public List<String> getChartLabels() { return chartLabels; }
    public void setChartLabels(List<String> chartLabels) { this.chartLabels = chartLabels; }
    public List<Map<String, Object>> getChartDatasets() { return chartDatasets; }
    public void setChartDatasets(List<Map<String, Object>> chartDatasets) { this.chartDatasets = chartDatasets; }

    public String getOverviewTitle() { return overviewTitle; }
    public void setOverviewTitle(String overviewTitle) { this.overviewTitle = overviewTitle; }
    public List<String[]> getOverview() { return overview; }
    public void setOverview(List<String[]> overview) { this.overview = overview; }

    public String getSideTitle() { return sideTitle; }
    public void setSideTitle(String sideTitle) { this.sideTitle = sideTitle; }
    public List<String[]> getSideItems() { return sideItems; }
    public void setSideItems(List<String[]> sideItems) { this.sideItems = sideItems; }

    public List<ReportTable> getTables() { return tables; }
    public void setTables(List<ReportTable> tables) { this.tables = tables; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
