<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="pageTitle" value="Lịch chiếu tổng hợp"/>
<c:set var="nav" value="schedule"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Lịch chiếu tổng hợp</h1>
        <p class="page-sub"><c:out value="${schedule.branch.name}"/> · ${schedule.fromStr} - ${schedule.toStr}</p>
    </div>
    <div class="actions">
        <a class="btn" href="${ctx}/manager/schedule?branchId=${branchId}&from=${schedule.prevIso}">&lsaquo; Tuần trước</a>
        <a class="btn" href="${ctx}/manager/schedule?branchId=${branchId}">Tuần này</a>
        <a class="btn" href="${ctx}/manager/schedule?branchId=${branchId}&from=${schedule.nextIso}">Tuần sau &rsaquo;</a>
    </div>
</div>

<c:if test="${not empty branches}">
    <form method="get" class="bk-toolbar">
        <label>Chi nhánh
            <select name="branchId" onchange="this.form.submit()">
                <c:forEach var="b" items="${branches}">
                    <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}><c:out value="${b.name}"/></option>
                </c:forEach>
            </select>
        </label>
        <input type="hidden" name="from" value="${schedule.fromIso}">
    </form>
</c:if>

<div class="bk-stats">
    <div class="bk-stat"><span>Suất chiếu</span><b>${schedule.totalShowtimes}</b></div>
    <div class="bk-stat"><span>Vé đã bán</span><b>${schedule.totalSold}</b></div>
    <div class="bk-stat"><span>Tổng ghế</span><b>${schedule.totalSeats}</b></div>
    <div class="bk-stat"><span>Tỷ lệ lấp đầy</span><b>${schedule.occupancy}%</b></div>
    <div class="bk-stat"><span>Suất đã hủy</span><b>${schedule.cancelledShowtimes}</b></div>
</div>

<div class="table-wrap" style="margin-bottom: 20px">
    <table class="bk-schedule">
        <thead>
        <tr>
            <th>Phòng</th>
            <c:forEach var="d" items="${schedule.days}"><th class="${d.today ? 'today' : ''}">${d.label}</th></c:forEach>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="row" items="${schedule.rows}">
            <tr>
                <th class="bk-room-cell"><c:out value="${row.room.roomName}"/>
                    <div class="meta"><c:out value="${row.room.roomType}"/> · ${row.room.seatCount} ghế</div>
                    <c:if test="${not row.room.active}"><span class="bk-badge cancelled">${row.room.statusLabel}</span></c:if></th>
                <c:forEach var="cell" items="${row.cells}" varStatus="vs">
                    <td class="${schedule.days[vs.index].today ? 'today' : ''}">
                        <c:forEach var="s" items="${cell}">
                            <a class="bk-slot ${s.statusClass}" href="${ctx}/manager/showtimes/edit?id=${s.showtimeId}"
                               title="${fn:escapeXml(s.movieTitle)} · ${s.displayStatus}">
                                <b>${s.startStr} - ${s.endStr}</b>
                                <span><c:out value="${s.movieTitle}"/></span>
                                <i>${s.soldCount}/${s.seatCount} · ${s.occupancy}%</i>
                            </a>
                        </c:forEach>
                        <a class="bk-slot-add" href="${ctx}/manager/showtimes/new?branchId=${branchId}&date=${schedule.days[vs.index].iso}" title="Thêm suất">+</a>
                    </td>
                </c:forEach>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<c:if test="${not empty schedule.movieStats}">
    <h2 class="section-title">Theo phim</h2>
    <div class="table-wrap">
        <table>
            <thead><tr><th>Phim</th><th class="bk-num">Số suất</th><th class="bk-num">Vé bán</th><th>Lấp đầy</th></tr></thead>
            <tbody>
            <c:forEach var="m" items="${schedule.movieStats}">
                <tr>
                    <td><c:out value="${m.title}"/></td>
                    <td class="bk-num">${m.showtimes}</td>
                    <td class="bk-num">${m.sold}/${m.seats}</td>
                    <td><div class="bk-bar"><span style="width:${m.occupancy}%"></span></div> <span class="meta">${m.occupancy}%</span></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
