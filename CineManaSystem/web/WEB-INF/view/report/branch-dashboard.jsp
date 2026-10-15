<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Tổng Quan - CineGo Management</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <style>
<%@ include file="/view/report/fragments/manager-shell-style.jsp" %>
        * { box-sizing: border-box; margin: 0; padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        body { background: #f1f5f9; color: #334155; font-size: 13px; min-height: 100vh; display: flex; flex-direction: column; }

        /* Khung noi dung chinh cua trang dashboard */
        .content { flex: 1; padding: 22px; overflow-y: auto; }
        .kpis { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 20px; }
        .card { background: #fff; border-radius: 14px; box-shadow: 0 1px 3px rgba(15,23,42,.08); padding: 20px 22px; }
        .kpi-label { font-size: 10px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: .3px; }
        .kpi-value { font-size: 26px; font-weight: 800; margin-top: 10px; }
        .kpi-value.green { color: #059669; }
        .kpi-value.red { color: #e11d48; }
        .kpi-sub { font-size: 10px; margin-top: 6px; color: #94a3b8; }
        .kpi-sub.up { color: #059669; font-weight: 700; }
        .kpi-sub.down { color: #dc2626; font-weight: 700; }
        .kpi-top { display: flex; justify-content: space-between; align-items: flex-start; }
        .kpi-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 16px; }
        .kpi-icon.green { background: #d1fae5; color: #059669; }
        .kpi-icon.red { background: #ffe4e6; color: #e11d48; }
        .gauge-head { display: flex; justify-content: space-between; align-items: center; }
        .gauge-pct { font-size: 15px; font-weight: 800; }
        .gauge-pct.good { color: #059669; } .gauge-pct.warn { color: #d97706; } .gauge-pct.bad { color: #e11d48; }
        .gauge-bar { height: 10px; background: #f1f5f9; border-radius: 999px; margin: 14px 0 10px; overflow: hidden; }
        .gauge-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #f59e0b, #e11d48); }
        .gauge-foot { display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; gap: 8px; }
        .gauge-foot b { color: #475569; }

        /* Khung widget danh sach suat chieu trong ngay */
        .widget { margin-top: 20px; }
        .widget-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
        .widget-title { font-size: 14px; font-weight: 800; color: #0f172a; display: flex; align-items: center; gap: 8px; }
        .widget-title i { color: #e11d48; }
        .widget-sub { font-size: 10px; color: #94a3b8; margin-top: 3px; }
        .widget-actions { display: flex; gap: 10px; align-items: center; }
        .btn-filter { background: #f1f5f9; color: #334155; border: 0; border-radius: 8px; padding: 8px 12px; font-size: 11px; font-weight: 700;
            display: inline-flex; align-items: center; gap: 6px; }
        .btn-filter select { border: 0; background: transparent; font-size: 11px; font-weight: 700; color: #334155; outline: none; cursor: pointer; }
        .btn-add { background: #e11d48; color: #fff; border: 0; border-radius: 8px; padding: 9px 14px; font-size: 11px; font-weight: 800; }
        .btn-add[disabled] { opacity: .45; cursor: not-allowed; }
        .table-wrap { overflow-x: auto; margin-top: 18px; }
        table { width: 100%; border-collapse: collapse; }
        th { font-size: 9px; font-weight: 800; text-transform: uppercase; color: #64748b; padding: 12px 14px; background: #f8fafc;
            border-bottom: 1px solid #e2e8f0; white-space: nowrap; text-align: left; }
        td { padding: 14px; border-bottom: 1px solid #f1f5f9; font-size: 11px; white-space: nowrap; }
        .center { text-align: center; } .right { text-align: right; }
        .show-title { font-weight: 800; color: #0f172a; font-size: 12px; }
        .show-sub { font-size: 9px; color: #94a3b8; margin-top: 3px; }
        .room { font-weight: 700; color: #0f172a; }
        .badge { display: inline-block; font-size: 8px; font-weight: 800; padding: 3px 8px; border-radius: 5px; }
        .fmt-imax { background: #f3e8ff; color: #7e22ce; }
        .fmt-3d { background: #fef3c7; color: #b45309; }
        .fmt-2d { background: #dbeafe; color: #1d4ed8; }
        .good { color: #059669; font-weight: 700; } .warn { color: #d97706; font-weight: 700; } .bad { color: #dc2626; font-weight: 700; }
        .rev { font-weight: 800; color: #0f172a; }
        .st { display: inline-block; font-size: 8px; font-weight: 800; padding: 3px 9px; border-radius: 999px; }
        .st-ongoing { background: #d1fae5; color: #047857; }
        .st-upcoming { background: #dbeafe; color: #1d4ed8; }
        .st-closed { background: #e2e8f0; color: #64748b; }
        .empty { text-align: center; color: #94a3b8; padding: 30px; }
        tr.hidden { display: none; }

        @media (max-width: 1000px) {
            .kpis { grid-template-columns: 1fr; }
            .sidebar { display: none; }
        }
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
            <!-- 3 the chi so KPI quan trong nhat: Doanh thu ngay, Doanh thu tuan, Ty le lap day ghe -->
            <div class="kpis">
                <section class="card">
                    <div class="kpi-top">
                        <div>
                            <div class="kpi-label">Revenue Today (Doanh thu hôm nay)</div>
                            <div class="kpi-value green"><c:out value="${dashboard.todayRevenue}"/></div>
                            <div class="kpi-sub ${dashboard.changeCss}">
                                <c:if test="${dashboard.changeCss == 'up'}"><i class="fa-solid fa-arrow-trend-up"></i></c:if>
                                <c:if test="${dashboard.changeCss == 'down'}"><i class="fa-solid fa-arrow-trend-down"></i></c:if>
                                <c:out value="${dashboard.changeText}"/>
                            </div>
                        </div>
                        <div class="kpi-icon green"><i class="fa-solid fa-wallet"></i></div>
                    </div>
                </section>

                <section class="card">
                    <div class="kpi-top">
                        <div>
                            <div class="kpi-label">Revenue This Week (Doanh thu tuần này)</div>
                            <div class="kpi-value red"><c:out value="${dashboard.weekRevenue}"/></div>
                            <div class="kpi-sub"><c:out value="${dashboard.weekShareText}"/></div>
                        </div>
                        <div class="kpi-icon red"><i class="fa-solid fa-chart-pie"></i></div>
                    </div>
                </section>

                <section class="card">
                    <div class="gauge-head">
                        <div class="kpi-label">Occupancy Gauge (Tỷ lệ lấp đầy)</div>
                        <div class="gauge-pct ${dashboard.occupancyCss}"><c:out value="${dashboard.occupancyText}"/></div>
                    </div>
                    <div class="gauge-bar"><div class="gauge-fill" style="width: ${dashboard.gaugeWidth}%"></div></div>
                    <div class="gauge-foot">
                        <span><c:out value="${dashboard.seatsText}"/></span>
                        <span><c:out value="${dashboard.roomsText}"/></span>
                    </div>
                </section>
            </div>

            <!-- Bang danh sach cac suat chieu hom nay cua chi nhanh -->
            <section class="card widget">
                <div class="widget-head">
                    <div>
                        <div class="widget-title"><i class="fa-regular fa-clock"></i> Today's Showtimes Widget (Danh sách Suất chiếu hôm nay)</div>
                        <div class="widget-sub">Ngày <c:out value="${dashboard.todayLabel}"/> &bull; cập nhật lúc <c:out value="${dashboard.updatedAt}"/> &bull;
                            theo dõi thời gian thực trạng thái lấp đầy và doanh thu từng suất chiếu.</div>
                    </div>
                    <div class="widget-actions">
                        <label class="btn-filter"><i class="fa-solid fa-filter"></i>
                            <select id="roomFilter">
                                <option value="">Lọc Theo Rạp</option>
                                <c:forEach var="room" items="${dashboard.roomOptions}">
                                    <option value="<c:out value='${room}'/>"><c:out value="${room}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <button type="button" class="btn-add" disabled title="Màn hình quản lý suất chiếu chưa được xây dựng">+ Thêm Suất Chiếu</button>
                    </div>
                </div>

                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>Suất chiếu &amp; Phim</th>
                                <th>Phòng chiếu</th>
                                <th>Định dạng</th>
                                <th class="center">Ghế (đã bán / tổng)</th>
                                <th class="center">Tỷ lệ lấp đầy (%)</th>
                                <th class="right">Doanh thu vé</th>
                                <th class="center">Trạng thái (Status)</th>
                            </tr>
                        </thead>
                        <tbody id="showBody">
                            <c:choose>
                                <c:when test="${empty dashboard.showtimes}">
                                    <tr><td colspan="7" class="empty">Hôm nay chưa có suất chiếu nào.</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="s" items="${dashboard.showtimes}">
                                        <tr data-room="<c:out value='${s.roomKey}'/>">
                                            <td>
                                                <div class="show-title"><c:out value="${s.timeLabel}"/> - <c:out value="${s.movieTitle}"/></div>
                                                <div class="show-sub"><c:out value="${s.durationText}"/></div>
                                            </td>
                                            <td>
                                                <div class="room"><c:out value="${s.roomLabel}"/></div>
                                                <c:if test="${not empty s.branchName}"><div class="show-sub"><c:out value="${s.branchName}"/></div></c:if>
                                            </td>
                                            <td><span class="badge ${s.formatCss}"><c:out value="${s.formatLabel}"/></span></td>
                                            <td class="center rev"><c:out value="${s.seatsText}"/></td>
                                            <td class="center ${s.occupancyCss}"><c:out value="${s.occupancyText}"/></td>
                                            <td class="right rev"><c:out value="${s.revenueText}"/></td>
                                            <td class="center"><span class="st st-${s.statusKey}"><c:out value="${s.statusLabel}"/></span></td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

</body>
</html>
