<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Tổng Quan - CineGo Management</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <%@ include file="/WEB-INF/view/report/fragments/manager-shell-style-tuyet.jsp" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/branch-dashboard.css">
</head>
<body>

    <div class="layout">
        <!-- Thanh menu dieu huong ben trai (Sidebar) -->
        <%@ include file="/WEB-INF/view/report/fragments/manager-sidebar-tuyet.jsp" %>

        <!-- Khung ben phai (navbar + noi dung), lui sang phai dung bang chieu rong sidebar co dinh -->
        <div class="main-wrapper">
            <%@ include file="/WEB-INF/view/report/fragments/manager-navbar-tuyet.jsp" %>

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
    </div>

</body>
</html>
