<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
        <!DOCTYPE html>
        <html lang="vi">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Báo Cáo Rạp (Branch Report Dashboard) - CineGo Management</title>

            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

            <%@ include file="/WEB-INF/view/report/fragments/manager-shell-style-tuyet.jsp" %>
                <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/branch-report.css">
        </head>

        <body>

            <!-- Thanh dieu huong tren cung (Navbar) -->
            <%@ include file="/WEB-INF/view/report/fragments/manager-navbar-tuyet.jsp" %>

                <div class="layout">
                    <!-- Thanh menu dieu huong ben trai (Sidebar) -->
                    <%@ include file="/WEB-INF/view/report/fragments/manager-sidebar-tuyet.jsp" %>

                        <!-- Vung noi dung chinh hien thi bao cao -->
                        <main class="content">

                            <div class="container">

                                <!-- Khoi tieu de trang va form bo loc thoi gian, chi nhanh -->
                                <section class="card">
                                    <div class="head-row">
                                        <div>
                                            <h1 class="title"><i class="fa-solid fa-chart-pie"></i> Báo Cáo Rạp (Branch
                                                Report Dashboard)</h1>
                                            <p class="subtitle">Branch Manager xem báo cáo doanh thu, tỷ lệ lấp đầy và
                                                hiệu suất suất chiếu của chi nhánh mình quản lý.</p>
                                        </div>

                                    </div>
                                    <hr class="divider">

                                    <c:if test="${not empty filterError}">
                                        <div class="alert-warn"><i class="fa-solid fa-triangle-exclamation"></i>
                                            <c:out value="${filterError}" />
                                        </div>
                                    </c:if>

                                    <form method="get" action="${pageContext.request.contextPath}/report/branch-report"
                                        id="filterForm" class="filters">
                                        <div class="field">
                                            <label>Từ ngày (Start Date)</label>
                                            <input type="date" name="fromDate" value="<c:out value='${fromDate}'/>"
                                                required onchange="this.form.submit()">
                                        </div>
                                        <div class="field">
                                            <label>Đến ngày (End Date)</label>
                                            <input type="date" name="toDate" value="<c:out value='${toDate}'/>" required
                                                onchange="this.form.submit()">
                                        </div>
                                        <div class="field">
                                            <label>Loại báo cáo (Report Type)</label>
                                            <select name="type" onchange="this.form.submit()">
                                                <c:forEach var="t" items="${typeLabels}">
                                                    <option value="${t.key}" ${t.key==report.type ? 'selected' : '' }>
                                                        <c:out value="${t.value}" />
                                                    </option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <c:if test="${not empty selectedBranchId}"><input type="hidden" name="branchId"
                                                value="${selectedBranchId}"></c:if>
                                        <a class="btn-csv"
                                            href="${pageContext.request.contextPath}/report/export-branch?type=${report.type}&fromDate=${fromDate}&toDate=${toDate}&branchId=${selectedBranchId}">
                                            <i class="fa-solid fa-file-csv"></i> Xuất CSV
                                        </a>
                                        <button type="button" class="btn-pdf" onclick="window.print()"><i
                                                class="fa-solid fa-file-pdf"></i> Xuất PDF</button>
                                        <noscript><button type="submit">Áp dụng</button></noscript>
                                    </form>
                                </section>

                                <!-- Khoi thong ke so lieu va tong quan hoat dong rap -->
                                <div class="grid">
                                    <section class="card">
                                        <div class="chart-head">
                                            <div class="chart-title">
                                                <c:out value="${report.chartTitle}" />
                                            </div>
                                            <div class="chart-unit">Đơn vị:
                                                <c:out value="${report.chartUnit}" />
                                            </div>
                                        </div>
                                        <div class="chart-box chart-box-compact">
                                            <div class="side-list">
                                                <c:forEach var="it" items="${report.sideItems}">
                                                    <div class="side-item">
                                                        <span class="side-item-name">
                                                            <c:out value="${it[0]}" />
                                                        </span>
                                                        <b class="side-item-value">
                                                            <c:out value="${it[1]}" />
                                                        </b>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    </section>

                                    <div class="side-col">
                                        <section class="card">
                                            <div class="side-title">
                                                <c:out value="${report.overviewTitle}" />
                                            </div>
                                            <c:forEach var="ln" items="${report.overview}">
                                                <div class="ov-line ${ln[2] == 'total' ? 'total' : ''}">
                                                    <span>
                                                        <c:out value="${ln[0]}" />
                                                    </span>
                                                    <b>
                                                        <c:out value="${ln[1]}" />
                                                    </b>
                                                </div>
                                            </c:forEach>
                                        </section>

                                        <section class="card">
                                            <div class="side-title">
                                                <c:out value="${report.sideTitle}" />
                                            </div>
                                            <c:choose>
                                                <c:when test="${empty report.sideItems}">
                                                    <div class="muted">Chưa có dữ liệu trong khoảng thời gian này.</div>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:forEach var="it" items="${report.sideItems}">
                                                        <div class="top-line"><span>
                                                                <c:out value="${it[0]}" />
                                                            </span><span>
                                                                <c:out value="${it[1]}" />
                                                            </span></div>
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
                                            <span>
                                                <c:out value="${tb.title}" />
                                            </span>
                                            <b>
                                                <c:out value="${tb.tag}" />
                                            </b>
                                        </div>
                                        <div class="table-wrap">
                                            <table>
                                                <thead>
                                                    <tr>
                                                        <c:forEach var="h" items="${tb.headers}" varStatus="hs">
                                                            <th class="al-${tb.aligns[hs.index]}">
                                                                <c:out value="${h}" />
                                                            </th>
                                                        </c:forEach>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <c:choose>
                                                        <c:when test="${empty tb.rows}">
                                                            <tr>
                                                                <td class="empty" colspan="${tb.headers.size()}">Không
                                                                    có dữ liệu trong khoảng thời gian này.</td>
                                                            </tr>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:forEach var="r" items="${tb.rows}">
                                                                <tr>
                                                                    <c:forEach var="cell" items="${r}" varStatus="cs">
                                                                        <td
                                                                            class="${cell[1]} al-${tb.aligns[cs.index]}">
                                                                            <c:out value="${cell[0]}" />
                                                                        </td>
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
                                    <p class="note"><i class="fa-solid fa-circle-info"></i>
                                        <c:out value="${report.note}" />
                                    </p>
                                </c:if>
                            </div>
                        </main>
                </div>

        </body>

        </html>