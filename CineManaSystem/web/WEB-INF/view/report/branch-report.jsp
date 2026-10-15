<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo Cáo Rạp (Branch Report Dashboard) - CineGo Management</title>

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
<%@ include file="/view/report/fragments/manager-shell-style.jsp" %>
        * { box-sizing: border-box; margin: 0; padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        body { background: #f1f5f9; color: #334155; font-size: 13px; min-height: 100vh; }

        .container { max-width: 1240px; margin: 0 auto; }
        .card { background: #fff; border-radius: 14px; box-shadow: 0 1px 3px rgba(15,23,42,.08); padding: 20px 22px; }

        /* Phan tieu de trang va form bo loc tim kiem */
        .head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
        .title { font-size: 19px; font-weight: 800; color: #0f172a; display: flex; align-items: center; gap: 8px; }
        .title i { color: #e11d48; }
        .subtitle { font-size: 11px; color: #64748b; margin-top: 4px; }
        .data-tag { background: #fef3c7; color: #92400e; font-weight: 700; font-size: 11px; padding: 4px 12px; border-radius: 999px; white-space: nowrap; }
        .divider { border: 0; border-top: 1px solid #e2e8f0; margin: 14px 0; }
        .alert-warn { background: #fffbeb; border: 1px solid #fde68a; color: #92400e; padding: 10px 14px; border-radius: 8px; margin-bottom: 14px; font-size: 12px; }
        .filters { display: grid; grid-template-columns: 1fr 1fr 1.6fr auto auto; gap: 14px; align-items: end; }
        .filters.with-branch { grid-template-columns: 1fr 1fr 1.6fr 1.2fr auto auto; }
        .field label { display: block; font-size: 10px; font-weight: 800; color: #64748b; text-transform: uppercase; margin-bottom: 5px; }
        .field input, .field select { width: 100%; height: 40px; border: 1px solid #cbd5e1; border-radius: 10px; padding: 0 12px;
            font-size: 12px; font-weight: 600; color: #0f172a; background: #f8fafc; }
        .btn-csv, .btn-pdf { height: 40px; border-radius: 10px; padding: 0 18px; font-weight: 700; font-size: 12px; cursor: pointer;
            display: inline-flex; align-items: center; justify-content: center; gap: 6px; text-decoration: none; white-space: nowrap; }
        .btn-csv { background: #f1f5f9; color: #334155; border: 1px solid #cbd5e1; }
        .btn-csv i { color: #059669; }
        .btn-pdf { background: #e11d48; color: #fff; border: 0; box-shadow: 0 4px 10px rgba(225,29,72,.25); }
        .btn-csv:hover { background: #e2e8f0; }
        .btn-pdf:hover { background: #be123c; }

        /* Khung luoi bo cuc: Thong ke so lieu va thong tin tong quan */
        .grid { display: grid; grid-template-columns: 1.9fr 1fr; gap: 20px; margin-top: 20px; align-items: start; }
        .chart-title { font-size: 13px; font-weight: 800; color: #0f172a; }
        .chart-unit { font-size: 10px; color: #94a3b8; }
        .chart-head { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; padding-bottom: 10px; }
        .chart-box { position: relative; height: 330px; margin-top: 10px; }
        .side-col { display: flex; flex-direction: column; gap: 20px; }
        .side-title { font-size: 13px; font-weight: 800; color: #0f172a; padding-bottom: 10px; border-bottom: 1px solid #f1f5f9; margin-bottom: 12px; }
        .ov-line { display: flex; justify-content: space-between; align-items: center; padding: 7px 0; font-size: 12px; color: #475569; }
        .ov-line b { color: #0f172a; font-size: 13px; }
        .ov-line.total { border-top: 1px solid #e2e8f0; margin-top: 6px; padding-top: 12px; color: #e11d48; font-weight: 800; font-size: 13px; }
        .ov-line.total b { color: #e11d48; font-size: 17px; }
        .top-line { display: flex; justify-content: space-between; gap: 10px; padding: 7px 0; font-size: 12px; font-weight: 800; color: #0f172a; }
        .top-line span:last-child { color: #e11d48; white-space: nowrap; }
        .muted { color: #94a3b8; font-size: 12px; padding: 6px 0; }

        /* Bang du lieu thong ke chi tiet */
        .table-card { margin-top: 20px; padding: 0; overflow: hidden; }
        .table-bar { display: flex; justify-content: space-between; align-items: center; padding: 14px 22px; background: #f8fafc;
            border-bottom: 1px solid #e2e8f0; font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: .3px; }
        .table-bar b { color: #0f172a; text-transform: none; font-size: 12px; }
        .table-wrap { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; }
        th { font-size: 10px; font-weight: 800; text-transform: uppercase; color: #64748b; padding: 14px 22px; border-bottom: 1px solid #e2e8f0; white-space: nowrap; }
        td { padding: 16px 22px; border-bottom: 1px solid #f1f5f9; font-size: 12px; white-space: nowrap; }
        .al-left { text-align: left; } .al-center { text-align: center; } .al-right { text-align: right; }
        td.bold { font-weight: 800; color: #0f172a; }
        td.good { color: #059669; } td.warn { color: #d97706; } td.bad { color: #dc2626; }
        td.total { color: #e11d48; font-weight: 800; }
        td.share { color: #059669; font-weight: 800; }
        td.empty { text-align: center; color: #94a3b8; padding: 28px; }
        .note { font-size: 11px; color: #94a3b8; margin-top: 16px; line-height: 1.5; }

        @media (max-width: 1000px) {
            .grid { grid-template-columns: 1fr; }
            .filters, .filters.with-branch { grid-template-columns: 1fr 1fr; }
        }
        @media print {
            .navbar, .btn-csv, .btn-pdf { display: none !important; }
            body { background: #fff; }
            .card { box-shadow: none; border: 1px solid #e2e8f0; page-break-inside: avoid; }
            .filters { grid-template-columns: 1fr 1fr 1.6fr; }
        }
    </style>
</head>
<body>

    <!-- Thanh dieu huong tren cung (Navbar) -->
    <%@ include file="/view/report/fragments/manager-navbar.jsp" %>

    <div class="layout">
        <!-- Thanh menu dieu huong ben trai (Sidebar) -->
        <%@ include file="/view/report/fragments/manager-sidebar.jsp" %>

        <!-- Vung noi dung chinh hien thi bao cao -->
        <main class="content">

    <div class="container">

        <!-- Khoi tieu de trang va form bo loc thoi gian, chi nhanh -->
        <section class="card">
            <div class="head-row">
                <div>
                    <h1 class="title"><i class="fa-solid fa-chart-pie"></i> Báo Cáo Rạp (Branch Report Dashboard)</h1>
                    <p class="subtitle">UC-30: Branch Manager xem báo cáo doanh thu, tỷ lệ lấp đầy và hiệu suất suất chiếu của chi nhánh mình quản lý (chỉ hiển thị dữ liệu của chi nhánh đó).</p>
                </div>
                <span class="data-tag"><c:out value="${report.dataTag}"/></span>
            </div>
            <hr class="divider">

            <c:if test="${not empty filterError}">
                <div class="alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> <c:out value="${filterError}"/></div>
            </c:if>

            <form method="get" action="${pageContext.request.contextPath}/report/branch-report" id="filterForm"
                  class="filters">
                <div class="field">
                    <label>Từ ngày (Start Date)</label>
                    <input type="date" name="fromDate" value="<c:out value='${fromDate}'/>" required onchange="this.form.submit()">
                </div>
                <div class="field">
                    <label>Đến ngày (End Date)</label>
                    <input type="date" name="toDate" value="<c:out value='${toDate}'/>" required onchange="this.form.submit()">
                </div>
                <div class="field">
                    <label>Loại báo cáo (Report Type)</label>
                    <select name="type" onchange="this.form.submit()">
                        <c:forEach var="t" items="${typeLabels}">
                            <option value="${t.key}" ${t.key == report.type ? 'selected' : ''}><c:out value="${t.value}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <c:if test="${not empty selectedBranchId}"><input type="hidden" name="branchId" value="${selectedBranchId}"></c:if>
                <a class="btn-csv" href="${pageContext.request.contextPath}/report/export-branch?type=${report.type}&fromDate=${fromDate}&toDate=${toDate}&branchId=${selectedBranchId}">
                    <i class="fa-solid fa-file-csv"></i> Xuất CSV
                </a>
                <button type="button" class="btn-pdf" onclick="window.print()"><i class="fa-solid fa-file-pdf"></i> Xuất PDF</button>
                <noscript><button type="submit">Áp dụng</button></noscript>
            </form>
        </section>

        <!-- Khoi thong ke so lieu va tong quan hoat dong rap -->
        <div class="grid">
            <section class="card">
                <div class="chart-head">
                    <div class="chart-title"><c:out value="${report.chartTitle}"/></div>
                    <div class="chart-unit">Đơn vị: <c:out value="${report.chartUnit}"/></div>
                </div>
                <div class="chart-box" style="height: auto; padding: 10px 0;">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <c:forEach var="it" items="${report.sideItems}">
                            <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 12px;">
                                <span style="font-weight: 700; color: #0f172a;"><c:out value="${it[0]}"/></span>
                                <b style="color: #e11d48;"><c:out value="${it[1]}"/></b>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </section>

            <div class="side-col">
                <section class="card">
                    <div class="side-title"><c:out value="${report.overviewTitle}"/></div>
                    <c:forEach var="ln" items="${report.overview}">
                        <div class="ov-line ${ln[2] == 'total' ? 'total' : ''}">
                            <span><c:out value="${ln[0]}"/></span>
                            <b><c:out value="${ln[1]}"/></b>
                        </div>
                    </c:forEach>
                </section>

                <section class="card">
                    <div class="side-title"><c:out value="${report.sideTitle}"/></div>
                    <c:choose>
                        <c:when test="${empty report.sideItems}">
                            <div class="muted">Chưa có dữ liệu trong khoảng thời gian này.</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="it" items="${report.sideItems}">
                                <div class="top-line"><span><c:out value="${it[0]}"/></span><span><c:out value="${it[1]}"/></span></div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </div>

        <!-- Cac bang du lieu thong ke chi tiet theo tung tieu chi -->
        <c:forEach var="tb" items="${report.tables}">
            <section class="card table-card">
                <div class="table-bar">
                    <span><c:out value="${tb.title}"/></span>
                    <b><c:out value="${tb.tag}"/></b>
                </div>
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <c:forEach var="h" items="${tb.headers}" varStatus="hs">
                                    <th class="al-${tb.aligns[hs.index]}"><c:out value="${h}"/></th>
                                </c:forEach>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty tb.rows}">
                                    <tr><td class="empty" colspan="${tb.headers.size()}">Không có dữ liệu trong khoảng thời gian này.</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="r" items="${tb.rows}">
                                        <tr>
                                            <c:forEach var="cell" items="${r}" varStatus="cs">
                                                <td class="${cell[1]} al-${tb.aligns[cs.index]}"><c:out value="${cell[0]}"/></td>
                                            </c:forEach>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </section>
        </c:forEach>

        <c:if test="${not empty report.note}">
            <p class="note"><i class="fa-solid fa-circle-info"></i> <c:out value="${report.note}"/></p>
        </c:if>
    </div>
        </main>
    </div>

</body>
</html>
