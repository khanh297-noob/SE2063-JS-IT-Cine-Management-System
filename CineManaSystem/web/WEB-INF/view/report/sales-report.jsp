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
        <style>
<%@ include file="/view/report/fragments/manager-shell-style.jsp" %>
            * {
                box-sizing: border-box;
                margin: 0;
                padding: 0;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            }

            body {
                background-color: #f8fafc;
                color: #334155;
                min-height: 100vh;
                display: flex;
                flex-direction: column;
                font-size: 13px;
            }

            .main-content {
                flex: 1;
                padding: 22px;
                overflow-y: auto;
                min-width: 0;
                background-color: #f8fafc;
            }

            /* Phan tieu de trang va nut xuat bao cao */
            .page-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 20px;
                padding-bottom: 12px;
                border-bottom: 1px solid #e2e8f0;
            }

            .page-title {
                font-size: 20px;
                font-weight: 800;
                color: #0f172a;
                display: flex;
                align-items: center;
                gap: 8px;
            }

            .page-title i {
                color: #e11d48;
            }

            .page-subtitle {
                font-size: 12px;
                color: #64748b;
                margin-top: 2px;
            }

            .btn-export {
                background-color: #059669;
                color: #ffffff;
                padding: 8px 16px;
                border-radius: 6px;
                text-decoration: none;
                font-weight: 700;
                display: inline-flex;
                align-items: center;
                gap: 6px;
                font-size: 12px;
                transition: 0.2s;
            }

            .btn-export:hover {
                background-color: #047857;
            }

            /* Khung the Card noi dung */
            .card {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 10px;
                padding: 16px 20px;
                margin-bottom: 20px;
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            }

            /* Form bo loc tim kiem xep theo chieu ngang */
            .filter-row {
                display: flex;
                flex-wrap: wrap;
                align-items: flex-end;
                gap: 16px;
                width: 100%;
            }

            .filter-group {
                display: flex;
                flex-direction: column;
                gap: 5px;
                flex: 1;
                min-width: 150px;
            }

            .filter-label {
                font-size: 10px;
                font-weight: 700;
                text-transform: uppercase;
                color: #64748b;
            }

            .filter-input, .filter-select {
                padding: 8px 12px;
                border: 1px solid #cbd5e1;
                border-radius: 6px;
                font-size: 12px;
                outline: none;
                background-color: #ffffff;
                width: 100%;
            }

            .filter-input:focus, .filter-select:focus {
                border-color: #e11d48;
            }

            .filter-actions {
                display: flex;
                align-items: center;
                gap: 8px;
                flex-shrink: 0;
            }

            .btn-filter {
                background-color: #e11d48;
                color: #ffffff;
                border: none;
                padding: 8px 18px;
                border-radius: 6px;
                font-weight: 700;
                cursor: pointer;
                font-size: 12px;
                display: inline-flex;
                align-items: center;
                gap: 6px;
                height: 33px;
            }

            .btn-filter:hover {
                background-color: #be123c;
            }

            .btn-reset {
                background-color: #f1f5f9;
                color: #475569;
                padding: 8px 12px;
                border-radius: 6px;
                text-decoration: none;
                font-weight: 700;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                height: 33px;
            }

            .btn-reset:hover {
                background-color: #e2e8f0;
            }

            /* Luoi 4 the hien thi chi so KPI tong quan */
            .kpi-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                gap: 16px;
                margin-bottom: 20px;
            }

            .kpi-card {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 10px;
                padding: 16px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-left: 4px solid #e11d48;
            }

            .kpi-card.green {
                border-left-color: #10b981;
            }
            .kpi-card.blue {
                border-left-color: #3b82f6;
            }
            .kpi-card.purple {
                border-left-color: #8b5cf6;
            }

            .kpi-title {
                font-size: 10px;
                font-weight: 700;
                text-transform: uppercase;
                color: #64748b;
                margin-bottom: 4px;
            }

            .kpi-value {
                font-size: 20px;
                font-weight: 800;
                color: #0f172a;
            }

            .kpi-subtext {
                font-size: 11px;
                color: #94a3b8;
                margin-top: 2px;
            }

            .kpi-icon {
                width: 40px;
                height: 40px;
                border-radius: 8px;
                background: #f8fafc;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 18px;
                color: #64748b;
            }

            /* Luoi 2 cot hien thi thong ke tien trinh */
            .charts-grid {
                display: grid;
                grid-template-columns: 2fr 1fr;
                gap: 16px;
                margin-bottom: 20px;
            }

            @media (max-width: 992px) {
                .charts-grid {
                    grid-template-columns: 1fr;
                }
            }

            .chart-header {
                font-size: 14px;
                font-weight: 700;
                color: #0f172a;
                margin-bottom: 12px;
                padding-bottom: 8px;
                border-bottom: 1px solid #f1f5f9;
            }

            /* Bieu do tien trinh bang CSS thuan, khong dung JavaScript */
            .stat-list {
                display: flex;
                flex-direction: column;
                gap: 10px;
                max-height: 250px;
                overflow-y: auto;
                padding-right: 6px;
            }

            .stat-row {
                display: flex;
                align-items: center;
                gap: 12px;
                font-size: 12px;
            }

            .stat-label {
                width: 100px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                font-weight: 600;
                color: #334155;
                flex-shrink: 0;
            }

            .stat-bar-container {
                flex: 1;
                height: 12px;
                background-color: #f1f5f9;
                border-radius: 999px;
                overflow: hidden;
            }

            .stat-bar-fill {
                height: 100%;
                border-radius: 999px;
                transition: width 0.3s;
            }

            .fill-red { background: linear-gradient(90deg, #f43f5e, #e11d48); }
            .fill-color-0 { background: #e11d48; }
            .fill-color-1 { background: #2563eb; }
            .fill-color-2 { background: #059669; }
            .fill-color-3 { background: #d97706; }
            .fill-color-4 { background: #9333ea; }
            .fill-color-5 { background: #ec4899; }

            .stat-count {
                width: 80px;
                text-align: right;
                font-size: 11px;
                color: #64748b;
                flex-shrink: 0;
            }

            .stat-val {
                width: 95px;
                text-align: right;
                font-weight: 700;
                color: #0f172a;
                flex-shrink: 0;
            }

            .stat-empty {
                text-align: center;
                padding: 40px;
                color: #94a3b8;
                font-size: 12px;
            }

            /* Bang danh sach chi tiet cac ve da ban */
            .table-container {
                overflow-x: auto;
            }

            table {
                width: 100%;
                border-collapse: collapse;
                text-align: left;
                font-size: 12px;
            }

            th {
                background-color: #f8fafc;
                color: #475569;
                font-weight: 700;
                padding: 10px 12px;
                border-bottom: 2px solid #e2e8f0;
                text-transform: uppercase;
                font-size: 10px;
                letter-spacing: 0.5px;
                white-space: nowrap;
            }

            td {
                padding: 10px 12px;
                border-bottom: 1px solid #f1f5f9;
                color: #334155;
                white-space: nowrap;
            }

            tr:hover td {
                background-color: #f8fafc;
            }

            .badge-status {
                background-color: #d1fae5;
                color: #047857;
                padding: 3px 8px;
                border-radius: 12px;
                font-size: 10px;
                font-weight: 700;
                display: inline-block;
            }

            .price-text {
                font-weight: 700;
                color: #059669;
            }

            .seat-badge {
                background: #f1f5f9;
                border: 1px solid #cbd5e1;
                padding: 2px 6px;
                border-radius: 4px;
                font-weight: 700;
            }
        </style>
    </head>
    <body>
        <%@ include file="/view/report/fragments/manager-navbar.jsp" %>
        <div class="layout">
            <%@ include file="/view/report/fragments/manager-sidebar.jsp" %>
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
                        <div class="kpi-value" style="color: #059669;">
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
                        <div class="kpi-value" style="font-size: 14px;">
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
                        <i class="fa-solid fa-chart-line" style="color: #e11d48;"></i> Xu hướng doanh thu theo ngày
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
                        <i class="fa-solid fa-chart-pie" style="color: #e11d48;"></i> Tỷ trọng theo Phim
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
                <div class="chart-header" style="display: flex; justify-content: space-between; align-items: center;">
                    <span><i class="fa-solid fa-list-check" style="color: #e11d48;"></i> Danh sách chi tiết vé đã bán</span>
                    <span style="font-size: 11px; color: #64748b; font-weight: normal;">
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
                                <th style="text-align: center;">Số ghế</th>
                                <th>Suất chiếu</th>
                                <th>Khách hàng</th>
                                <th style="text-align: right;">Giá vé (VNĐ)</th>
                                <th style="text-align: center;">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty reportItems}">
                                    <c:forEach var="item" items="${reportItems}">
                                        <tr>
                                            <td style="font-weight: 700; color: #e11d48;">#${item.ticketCode}</td>
                                            <td style="font-weight: 700; color: #0f172a;">${item.movieTitle}</td>
                                            <td>${item.branchName}</td>
                                            <td>${item.roomName}</td>
                                            <td style="text-align: center;"><span class="seat-badge">${item.seatName}</span></td>
                                            <td>
                                                <fmt:formatDate value="${item.showDate}" pattern="dd/MM/yyyy"/>
                                                <span style="color: #94a3b8; font-size: 11px;">(<fmt:formatDate value="${item.startTime}" pattern="HH:mm"/>)</span>
                                            </td>
                                            <td>
                                                <strong>${item.customerName}</strong>
                                                <c:if test="${not empty item.customerEmail}">
                                                    <div style="font-size: 10px; color: #94a3b8;">${item.customerEmail}</div>
                                                </c:if>
                                            </td>
                                            <td style="text-align: right;" class="price-text">
                                                <fmt:formatNumber value="${item.finalPrice}" pattern="#,###"/> ₫
                                            </td>
                                            <td style="text-align: center;">
                                                <span class="badge-status">${item.ticketStatus}</span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="9" style="text-align: center; padding: 32px; color: #94a3b8;">
                                            <i class="fa-solid fa-folder-open" style="font-size: 28px; margin-bottom: 8px; display: block;"></i>
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

</body>
</html>