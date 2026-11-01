<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Báo cáo Doanh thu & Bán vé - CineGo Management</title>

        <!-- Thu vien icon FontAwesome -->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

        <!-- CSS thuan cho toan bo giao dien trang bao cao -->
        <%@ include file="/WEB-INF/view/report/fragments/manager-shell-style-tuyet.jsp" %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sales-report.css">
    </head>
    <body>
        <div class="layout">
            <%@ include file="/WEB-INF/view/report/fragments/manager-sidebar-tuyet.jsp" %>

            <!-- Khung ben phai (navbar + noi dung), lui sang phai dung bang chieu rong sidebar co dinh -->
            <div class="main-wrapper">
                <%@ include file="/WEB-INF/view/report/fragments/manager-navbar-tuyet.jsp" %>
            <!-- Vung noi dung chinh ben phai -->
            <main class="content main-content">

            <!-- Dong tieu de trang va nut xuat du lieu Excel/CSV -->
            <div class="page-header">
                <div>
                    <h1 class="page-title">
                        <i class="fa-solid fa-chart-pie"></i> Báo cáo Doanh thu & Bán vé
                    </h1>
                    <p class="page-subtitle">Theo dõi chi tiết số lượng vé và doanh số bán ra theo từng rạp, phim</p>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/report/export-sales?branchId=${selectedBranchId}&movieId=${selectedMovieId}&fromDate=${selectedFromDate}&toDate=${selectedToDate}" class="btn-export">
                        <i class="fa-solid fa-file-excel"></i> Xuất Excel / CSV
                    </a>
                </div>
            </div>


            <!-- Form bo loc du lieu: rap, phim, khoang thoi gian -->
            <div class="card">
                <form action="${pageContext.request.contextPath}/report/ticket-sales" method="get" class="filter-row">

                    <div class="filter-group">
                        <label class="filter-label">Chi nhánh rạp</label>
                        <select name="branchId" class="filter-select">
                            <option value="">-- Tất cả chi nhánh --</option>
                            <c:forEach var="b" items="${branches}">
                                <option value="${b.branchID}" ${selectedBranchId eq b.branchID ? 'selected' : ''}>${b.name}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="filter-group">
                        <label class="filter-label">Phim chiếu</label>
                        <select name="movieId" class="filter-select">
                            <option value="">-- Tất cả phim --</option>
                            <c:forEach var="m" items="${movies}">
                                <option value="${m.movieID}" ${selectedMovieId eq m.movieID ? 'selected' : ''}>${m.title}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="filter-group">
                        <label class="filter-label">Từ ngày</label>
                        <input type="date" name="fromDate" value="${selectedFromDate}" class="filter-input">
                    </div>

                    <div class="filter-group">
                        <label class="filter-label">Đến ngày</label>
                        <input type="date" name="toDate" value="${selectedToDate}" class="filter-input">
                    </div>

                    <div class="filter-actions">
                        <button type="submit" class="btn-filter">
                            <i class="fa-solid fa-filter"></i> Lọc
                        </button>
                        <a href="${pageContext.request.contextPath}/report/ticket-sales" class="btn-reset" title="Đặt lại">
                            <i class="fa-solid fa-rotate-left"></i>
                        </a>
                    </div>

                </form>
            </div>

            <!-- Cac the chi so KPI tong quan: Doanh thu, So ve, Luot dat, Phim hot -->
            <div class="kpi-grid">
                <div class="kpi-card green">
                    <div>
                        <div class="kpi-title">Tổng doanh thu vé</div>
                        <div class="kpi-value kpi-green">
                            <fmt:formatNumber value="${summary.totalRevenue}" type="currency" currencySymbol="₫" maxFractionDigits="0"/>
                        </div>
                        <div class="kpi-subtext">Doanh số thực thu từ vé bán</div>
                    </div>
                    <div class="kpi-icon"><i class="fa-solid fa-money-bill-wave"></i></div>
                </div>

                <div class="kpi-card">
                    <div>
                        <div class="kpi-title">Tổng số vé đã bán</div>
                        <div class="kpi-value">
                            <fmt:formatNumber value="${summary.totalTickets}" pattern="#,###"/> Vé
                        </div>
                        <div class="kpi-subtext">Hóa đơn xuất thành công</div>
                    </div>
                    <div class="kpi-icon"><i class="fa-solid fa-ticket"></i></div>
                </div>

                <div class="kpi-card blue">
                    <div>
                        <div class="kpi-title">Số lượt đơn đặt vé</div>
                        <div class="kpi-value">
                            <fmt:formatNumber value="${summary.totalBookings}" pattern="#,###"/>
                        </div>
                        <div class="kpi-subtext">Giao dịch phát sinh</div>
                    </div>
                    <div class="kpi-icon"><i class="fa-solid fa-receipt"></i></div>
                </div>

                <div class="kpi-card purple">
                    <div>
                        <div class="kpi-title">Phim bán chạy nhất</div>
                        <div class="kpi-value kpi-small">
                            ${summary.topMovieTitle}
                        </div>
                        <div class="kpi-subtext">Đạt <strong>${summary.topMovieTickets}</strong> vé đã bán</div>
                    </div>
                    <div class="kpi-icon"><i class="fa-solid fa-trophy"></i></div>
                </div>
            </div>

            <!-- Luoi thong ke truc quan bang CSS thuan, khong dung JavaScript -->
            <div class="charts-grid">
                <!-- Cot 1: Thong ke doanh thu theo tung ngay -->
                <div class="card">
                    <div class="chart-header">
                        <i class="fa-solid fa-chart-line icon-red"></i> Xu hướng doanh thu theo ngày
                    </div>
                    <div class="stat-list">
                        <c:choose>
                            <c:when test="${not empty dailyRevenue}">
                                <c:set var="maxDailyRev" value="1" />
                                <c:forEach var="d" items="${dailyRevenue}">
                                    <c:if test="${d[2] > maxDailyRev}">
                                        <c:set var="maxDailyRev" value="${d[2]}" />
                                    </c:if>
                                </c:forEach>
                                <c:forEach var="d" items="${dailyRevenue}">
                                    <c:set var="dailyPct" value="${(d[2] / maxDailyRev) * 100}" />
                                    <div class="stat-row">
                                        <span class="stat-label">${d[0]}</span>
                                        <div class="stat-bar-container">
                                            <div class="stat-bar-fill fill-red" style="width: ${dailyPct > 0 ? (dailyPct < 4 ? 4 : dailyPct) : 0}%;"></div>
                                        </div>
                                        <span class="stat-count">${d[1]} vé</span>
                                        <span class="stat-val"><fmt:formatNumber value="${d[2]}" pattern="#,###"/> ₫</span>
                                    </div>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="stat-empty">Chưa có dữ liệu theo ngày</div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Cot 2: Thong ke ty trong doanh thu theo tung bo phim -->
                <div class="card">
                    <div class="chart-header">
                        <i class="fa-solid fa-chart-pie icon-red"></i> Tỷ trọng theo Phim
                    </div>
                    <div class="stat-list">
                        <c:choose>
                            <c:when test="${not empty movieRevenue}">
                                <c:set var="sumMovieRev" value="${summary.totalRevenue > 0 ? summary.totalRevenue : 1}" />
                                <c:forEach var="m" items="${movieRevenue}" varStatus="status">
                                    <c:set var="mPct" value="${(m[2] / sumMovieRev) * 100}" />
                                    <div class="stat-row">
                                        <span class="stat-label" title="${m[0]}">${m[0]}</span>
                                        <div class="stat-bar-container">
                                            <div class="stat-bar-fill fill-color-${status.index % 6}" style="width: ${mPct > 0 ? (mPct < 4 ? 4 : mPct) : 0}%;"></div>
                                        </div>
                                        <span class="stat-count"><fmt:formatNumber value="${mPct}" maxFractionDigits="1"/>% (${m[1]} vé)</span>
                                        <span class="stat-val"><fmt:formatNumber value="${m[2]}" pattern="#,###"/> ₫</span>
                                    </div>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="stat-empty">Chưa có dữ liệu theo phim</div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Bang chi tiet danh sach tung ve da xuat thanh cong -->
            <div class="card">
                <div class="chart-header chart-header-flex">
                    <span><i class="fa-solid fa-list-check icon-red"></i> Danh sách chi tiết vé đã bán</span>
                    <span class="chart-header-note">
                        Tổng cộng: <strong>${reportItems.size()}</strong> vé
                    </span>
                </div>

                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Mã vé</th>
                                <th>Tên phim</th>
                                <th>Rạp / Chi nhánh</th>
                                <th>Phòng</th>
                                <th class="align-center">Số ghế</th>
                                <th>Suất chiếu</th>
                                <th>Khách hàng</th>
                                <th class="align-right">Giá vé (VNĐ)</th>
                                <th class="align-center">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty reportItems}">
                                    <c:forEach var="item" items="${reportItems}">
                                        <tr>
                                            <td class="cell-ticket-code">#${item.ticketCode}</td>
                                            <td class="cell-movie">${item.movieTitle}</td>
                                            <td>${item.branchName}</td>
                                            <td>${item.roomName}</td>
                                            <td class="align-center"><span class="seat-badge">${item.seatName}</span></td>
                                            <td>
                                                <fmt:formatDate value="${item.showDate}" pattern="dd/MM/yyyy"/>
                                                <span class="cell-time">(<fmt:formatDate value="${item.startTime}" pattern="HH:mm"/>)</span>
                                            </td>
                                            <td>
                                                <strong>${item.customerName}</strong>
                                                <c:if test="${not empty item.customerEmail}">
                                                    <div class="cell-email">${item.customerEmail}</div>
                                                </c:if>
                                            </td>
                                            <td class="price-text align-right">
                                                <fmt:formatNumber value="${item.finalPrice}" pattern="#,###"/> ₫
                                            </td>
                                            <td class="align-center">
                                                <span class="badge-status">${item.ticketStatus}</span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="9" class="empty-cell">
                                            <i class="fa-solid fa-folder-open empty-icon"></i>
                                            Không tìm thấy vé nào phù hợp với điều kiện lọc đã chọn.
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

        </main>
        </div>
    </div>

</body>
</html>