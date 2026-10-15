package dto;

import java.util.ArrayList;
import java.util.List;

/** Bảng dữ liệu tổng quát cho trang báo cáo chi nhánh. Mỗi ô: {hiển thị, css class, giá trị thô cho CSV}. */
public class ReportTable {

    private final String title;
    private String tag = "";
    private final List<String> headers;
    private final List<String> aligns;
    private final List<List<String[]>> rows = new ArrayList<>();

    public ReportTable(String title, List<String> headers, List<String> aligns) {
        this.title = title;
        this.headers = headers;
        this.aligns = aligns;
    }

    public static String[] cell(String text) {
        return new String[]{text, "", text};
    }

    public static String[] cell(String text, String css) {
        return new String[]{text, css, text};
    }

    public static String[] cell(String text, String css, String raw) {
        return new String[]{text, css, raw};
    }

    public void addRow(List<String[]> row) { rows.add(row); }

    public String getTitle() { return title; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public List<String> getHeaders() { return headers; }
    public List<String> getAligns() { return aligns; }
    public List<List<String[]>> getRows() { return rows; }
}
