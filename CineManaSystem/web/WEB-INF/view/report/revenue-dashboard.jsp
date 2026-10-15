<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phân Tích Doanh Thu Chi Tiết - CineGo Management</title>

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
<%@ include file="/view/report/fragments/manager-shell-style.jsp" %>
        * { box-sizing: border-box; margin: 0; padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        body { background-color: #f8fafc; color: #334155; min-height: 100vh; display: flex; flex-direction: column; font-size: 13px; }

        /* Khung the Card noi dung */
        .card { background: #fff; border-radius: 14px; box-shadow: 0 1px 3px rgba(15,23,42,.08); padding: 20px 22px; margin-bottom: 20px; }
        .head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
        .page-title { font-size: 18px; font-weight: 800; color: #0f172a; display: flex; align-items: center; gap: 8px; }
        .page-title i { color: #e11d48; }
        .page-subtitle { font-size: 11px; color: #64748b; margin-top: 4px; }
        .scope-badge { background: #d1fae5; color: #047857; font-weight: 700; font-size: 11px; padding: 4px 12px; border-radius: 999px; white-space: nowrap; }
        .divider { border: 0; border-top: 1px solid #e2e8f0; margin: 14px 0; }

        /* Form bo loc thoi gian va nut xuat bao cao */
        .filter-row { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
        .filter-left { display: flex; align-items: flex-end; gap: 10px; flex-wrap: wrap; }
        .field label { display: block; font-size: 10px; font-weight: 800; color: #64748b; text-transform: uppercase; margin-bottom: 4px; }
        .field label .req { color: #e11d48; }
        .field input, .field select { height: 36px; border: 1px solid #cbd5e1; border-radius: 8px; padding: 0 10px; font-size: 12px; color: #0f172a; background: #fff; }
        .sep { color: #94a3b8; font-size: 12px; padding-bottom: 10px; }
        .btn-apply { height: 36px; background: #0f172a; color: #fff; border: 0; border-radius: 8px; padding: 0 16px; font-weight: 700; font-size: 12px; cursor: pointer; }
        .btn-apply:hover { background: #1e293b; }
        .filter-right { display: flex; gap: 10px; }
        .btn-csv, .btn-pdf { height: 36px; border-radius: 8px; padding: 0 14px; font-weight: 700; font-size: 12px; cursor: pointer;
            display: inline-flex; align-items: center; gap: 6px; text-decoration: none; }
        .btn-csv { background: #fff; color: #334155; border: 1px solid #cbd5e1; }
        .btn-csv i { color: #059669; }
        .btn-csv:hover { background: #f1f5f9; }
        .btn-pdf { background: #e11d48; color: #fff; border: 0; }
        .btn-pdf:hover { background: #be123c; }
        .alert-warn { background: #fffbeb; border: 1px solid #fde68a; color: #92400e; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; font-size: 12px; }

        /* Khung thong ke bieu do tang truong */
        .chart-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
        .chart-title { font-size: 15px; font-weight: 800; color: #0f172a; }
        .chart-sub { font-size: 11px; color: #94a3b8; margin-top: 2px; }
        .legend { display: flex; gap: 16px; font-size: 11px; font-weight: 700; color: #334155; }
        .legend span { display: inline-flex; align-items: center; gap: 6px; }
        .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
        .chart-box { position: relative; height: 300px; margin-top: 12px; }

        /* Bang phan ra chi tiet doanh thu */
        .table-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
        /* Dropdown chon loai bang phan ra */
        .breakdown-form { display: flex; align-items: flex-end; }
        .breakdown-form select { min-width: 200px; }
        .tab-panel { display: block; margin-bottom: 24px; }
        .table-wrap { overflow-x: auto; }
        table.breakdown { width: 100%; border-collapse: collapse; }
        table.breakdown th { background: #f8fafc; color: #64748b; font-size: 10px; font-weight: 800; text-transform: uppercase;
            padding: 12px 14px; text-align: right; border-bottom: 1px solid #e2e8f0; white-space: nowrap; }
        table.breakdown th:first-child, table.breakdown td:first-child { text-align: left; }
        table.breakdown td { padding: 14px; text-align: right; border-bottom: 1px solid #f1f5f9; font-size: 12px; white-space: nowrap; }
        table.breakdown td.name { font-weight: 800; color: #0f172a; text-transform: uppercase; }
        table.breakdown td.total { color: #e11d48; font-weight: 800; }
        table.breakdown td.tickets { text-align: center; font-weight: 700; }
        table.breakdown th.tickets-h { text-align: center; }
        table.breakdown td.share { font-weight: 800; color: #059669; }
        table.breakdown tr.sum td { background: #f8fafc; font-weight: 800; border-top: 2px solid #e2e8f0; }
        .empty { text-align: center; color: #94a3b8; padding: 28px 0; }
        .note { font-size: 11px; color: #94a3b8; margin-top: 10px; }

        @media print {
            .navbar, .sidebar, .filter-right, .btn-apply, .breakdown-form { display: none !important; }
            .layout-container { display: block; }
            .main-content { padding: 0; overflow: visible; }
            .card { box-shadow: none; border: 1px solid #e2e8f0; page-break-inside: avoid; }
            .tab-panel { display: block !important; margin-bottom: 16px; }
            .tab-title-print { display: block !important; }
            body { background: #fff; }
        }
        .tab-title-print { display: none; font-weight: 800; color: #0f172a; margin: 12px 0 6px; }
    </style>
</head>
<body>

    <!-- Thanh dieu huong tren cung (Navbar) -->
    <%@ include file="/view/report/fragments/manager-navbar.jsp" %>

    <div class="layout">
        <!-- Thanh menu dieu huong ben trai (Sidebar) -->
        <%@ include file="/view/report/fragments/manager-sidebar.jsp" %>

        <!-- Vung noi dung chinh ben phai -->
        <main class="content">

            <!-- Khoi tieu de trang va pham vi du lieu chi nhanh -->
            <section class="card">
                <div class="head-row">
                    <div>
                        <h1 class="page-title"><i class="fa-solid fa-chart-column"></i> Phân Tích Doanh Thu Chi Tiết (Revenue Dashboard)</h1>
                        <p class="page-subtitle">Biểu đồ và bảng doanh thu của chi nhánh, phân rã theo Phim, Loại ghế và Phương thức thanh toán (gồm F&amp;B đi kèm). Chỉ hiển thị dữ liệu thuộc chi nhánh do manager quản lý.</p>
                    </div>
                    <span class="scope-badge"><c:out value="${dashboard.scopeLabel}"/></span>
                </div>
                <hr class="divider">

                <c:if test="${not empty filterError}">
                    <div class="alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> <c:out value="${filterError}"/></div>
                </c:if>

                <div class="filter-row">
                    <form method="get" action="${pageContext.request.contextPath}/report/revenue-dashboard" class="filter-left">
                        <div class="field">
                            <label>Từ ngày <span class="req">*</span></label>
                            <input type="date" name="fromDate" value="<c:out value='${fromDate}'/>" required>
                        </div>
                        <span class="sep">đến</span>
                        <div class="field">
                            <label>Đến ngày <span class="req">*</span></label>
                            <input type="date" name="toDate" value="<c:out value='${toDate}'/>" required>
                        </div>
                        <c:if test="${not empty selectedBranchId}"><input type="hidden" name="branchId" value="${selectedBranchId}"></c:if>
                        <button type="submit" class="btn-apply">Áp Dụng Lọc</button>
                    </form>

                    <div class="filter-right">
                        <a class="btn-csv" href="${pageContext.request.contextPath}/report/export-revenue?fromDate=${fromDate}&toDate=${toDate}&branchId=${selectedBranchId}">
                            <i class="fa-solid fa-file-csv"></i> Xuất Báo Cáo CSV
                        </a>
                        <button type="button" class="btn-pdf" onclick="window.print()">
                            <i class="fa-solid fa-file-pdf"></i> Xuất Báo Cáo PDF
                        </button>
                    </div>
                </div>
            </section>

            <!-- Thong ke tang truong doanh thu ve va F&B theo tuan (CSS thuan) -->
            <section class="card">
                <div class="chart-head">
                    <div>
                        <div class="chart-title">Biểu Đồ Tăng Trưởng Doanh Thu (Vé vs F&amp;B Combo)</div>
                        <div class="chart-sub">So sánh doanh thu bán vé và dịch vụ bắp nước theo thời gian</div>
                    </div>
                    <div class="legend">
                        <span><i class="dot" style="background:#e11d48"></i> Doanh thu Vé</span>
                        <span><i class="dot" style="background:#f59e0b"></i> Doanh thu F&amp;B</span>
                    </div>
                </div>
                <div class="chart-box" style="height: auto; padding: 10px 0;">
                    <div style="display: flex; flex-direction: column; gap: 10px;">
                        <c:forEach var="w" items="${dashboard.weekLabels}" varStatus="ws">
                            <c:set var="tRev" value="${dashboard.weeklyTicketRevenue[ws.index]}" />
                            <c:set var="fRev" value="${dashboard.weeklyFnbRevenue[ws.index]}" />
                            <c:set var="sumRev" value="${tRev + fRev}" />
                            <div style="display: flex; align-items: center; gap: 12px; font-size: 12px;">
                                <span style="width: 140px; font-weight: 700; color: #334155;">${w}</span>
                                <div style="flex: 1; height: 16px; background: #f1f5f9; border-radius: 999px; overflow: hidden; display: flex;">
                                    <div style="background: #e11d48; height: 100%; width: ${sumRev > 0 ? (tRev / sumRev) * 100 : 0}%;" title="Vé: <fmt:formatNumber value='${tRev}' pattern='#,##0'/> ₫"></div>
                                    <div style="background: #f59e0b; height: 100%; width: ${sumRev > 0 ? (fRev / sumRev) * 100 : 0}%;" title="F&B: <fmt:formatNumber value='${fRev}' pattern='#,##0'/> ₫"></div>
                                </div>
                                <span style="width: 160px; text-align: right; font-weight: 700; color: #0f172a;">
                                    <fmt:formatNumber value="${sumRev}" pattern="#,##0"/> ₫
                                </span>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </section>

            <!-- Bang phan ra doanh thu chi tiet - chon loai bang qua dropdown -->
            <section class="card">
                <div class="table-head">
                    <div class="chart-title">Bảng Phân Rã Chi Tiết (Breakdown Tables)</div>

                    <!-- Form chon loai bang phan ra: gui lai trang voi tham so breakdown -->
                    <form method="get" action="${pageContext.request.contextPath}/report/revenue-dashboard" class="breakdown-form">
                        <!-- Giu lai cac tham so loc hien tai khi chuyen loai bang -->
                        <input type="hidden" name="fromDate" value="<c:out value='${fromDate}'/>">
                        <input type="hidden" name="toDate" value="<c:out value='${toDate}'/>">
                        <c:if test="${not empty selectedBranchId}">
                            <input type="hidden" name="branchId" value="${selectedBranchId}">
                        </c:if>

                        <div class="field">
                            <label>Phân rã theo</label>
                            <select name="breakdown" onchange="this.form.submit()">
                                <c:forEach var="tb" items="${dashboard.tables}">
                                    <option value="${tb.key}" ${selectedBreakdown == tb.key ? 'selected' : ''}>
                                        <c:out value="${tb.title}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </form>
                </div>

                <!-- Chi hien thi bang tuong ung voi loai da chon trong dropdown -->
                <c:forEach var="tb" items="${dashboard.tables}">
                    <c:if test="${tb.key == selectedBreakdown}">
                        <div class="tab-panel active" id="tab-${tb.key}">
                            <div class="tab-title-print"><c:out value="${tb.title}"/></div>
                            <div class="table-wrap">
                                <table class="breakdown">
                                    <thead>
                                        <tr>
                                            <th><c:out value="${tb.nameHeader}"/></th>
                                            <th class="tickets-h">Tổng số vé bán</th>
                                            <th>Doanh thu vé (VND)</th>
                                            <th>Doanh thu F&amp;B đi kèm</th>
                                            <th><c:out value="${tb.totalHeader}"/></th>
                                            <th>Tỷ trọng (%)</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:choose>
                                            <c:when test="${empty tb.rows}">
                                                <tr><td colspan="6" class="empty">Không có dữ liệu trong khoảng thời gian này.</td></tr>
                                            </c:when>
                                            <c:otherwise>
                                                <c:forEach var="r" items="${tb.rows}">
                                                    <tr>
                                                        <td class="name"><c:out value="${r.name}"/></td>
                                                        <td class="tickets"><fmt:formatNumber value="${r.tickets}" pattern="#,##0"/></td>
                                                        <td><fmt:formatNumber value="${r.ticketRevenue}" pattern="#,##0"/> ₫</td>
                                                        <td><fmt:formatNumber value="${r.fnbRevenue}" pattern="#,##0"/> ₫</td>
                                                        <td class="total"><fmt:formatNumber value="${r.totalRevenue}" pattern="#,##0"/> ₫</td>
                                                        <td class="share"><fmt:formatNumber value="${r.share}" pattern="0.0"/>%</td>
                                                    </tr>
                                                </c:forEach>
                                                <tr class="sum">
                                                    <td>Tổng cộng</td>
                                                    <td class="tickets"><fmt:formatNumber value="${tb.totalTickets}" pattern="#,##0"/></td>
                                                    <td><fmt:formatNumber value="${tb.totalTicketRevenue}" pattern="#,##0"/> ₫</td>
                                                    <td><fmt:formatNumber value="${tb.totalFnbRevenue}" pattern="#,##0"/> ₫</td>
                                                    <td class="total"><fmt:formatNumber value="${tb.grandTotal}" pattern="#,##0"/> ₫</td>
                                                    <td>100.0%</td>
                                                </tr>
                                            </c:otherwise>
                                        </c:choose>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>

                <p class="note">
                    Chỉ tính vé còn hiệu lực của booking đã xác nhận và thanh toán thành công. Doanh thu F&amp;B của mỗi booking được
                    chia đều cho các vé của booking đó và tính theo ngày chiếu; F&amp;B dùng đơn giá hiện tại trong danh mục F&amp;B.
                </p>
            </section>
        </main>
    </div>

</body>
</html>
