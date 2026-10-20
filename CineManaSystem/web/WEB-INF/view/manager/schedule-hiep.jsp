<c:set var="pageTitle" value="Lịch chiếu tổng hợp"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="page-head">
    <div>
        <h1 class="page-title">Lịch chiếu tổng hợp</h1>
        <p class="muted">${fn:escapeXml(schedule.branch.name)} · ${schedule.fromStr} – ${schedule.toStr}</p>
    </div>
    <div class="actions">
        <a class="btn btn-ghost" href="${ctx}/manager/schedule?branchId=${branchId}&from=${schedule.prevIso}">‹ Tuần trước</a>
        <a class="btn btn-ghost" href="${ctx}/manager/schedule?branchId=${branchId}">Tuần này</a>
        <a class="btn btn-ghost" href="${ctx}/manager/schedule?branchId=${branchId}&from=${schedule.nextIso}">Tuần sau ›</a>
    </div>
</div>
<c:if test="${not empty branches}">
    <form method="get" class="filter-bar">
        <label>Chi nhánh
            <select name="branchId" onchange="this.form.submit()">
                <c:forEach var="b" items="${branches}">
                    <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}>${fn:escapeXml(b.name)}</option>
                </c:forEach>
            </select>
        </label>
        <input type="hidden" name="from" value="${schedule.fromIso}">
    </form>
</c:if>

<div class="stats">
    <div class="stat"><span>Suất chiếu</span><b>${schedule.totalShowtimes}</b></div>
    <div class="stat"><span>Vé đã bán</span><b>${schedule.totalSold}</b></div>
    <div class="stat"><span>Tổng ghế</span><b>${schedule.totalSeats}</b></div>
    <div class="stat"><span>Tỷ lệ lấp đầy</span><b>${schedule.occupancy}%</b></div>
    <div class="stat"><span>Suất đã hủy</span><b>${schedule.cancelledShowtimes}</b></div>
</div>

<div class="table-wrap">
<table class="schedule">
    <thead>
    <tr>
        <th>Phòng</th>
        <c:forEach var="d" items="${schedule.days}"><th class="${d.today ? 'today' : ''}">${d.label}</th></c:forEach>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="row" items="${schedule.rows}">
        <tr>
            <th class="room-cell">${fn:escapeXml(row.room.roomName)}<div class="muted small">${fn:escapeXml(row.room.roomType)} · ${row.room.seatCount} ghế</div>
                <c:if test="${not row.room.active}"><span class="badge badge-cancelled">Bảo trì</span></c:if></th>
            <c:forEach var="cell" items="${row.cells}" varStatus="vs">
                <td class="${schedule.days[vs.index].today ? 'today' : ''}">
                    <c:forEach var="s" items="${cell}">
                        <a class="slot slot-${s.statusClass}" href="${ctx}/manager/showtimes/edit?id=${s.showtimeId}"
                           title="${fn:escapeXml(s.movieTitle)} · ${s.displayStatus}">
                            <b>${s.startStr}–${s.endStr}</b>
                            <span>${fn:escapeXml(s.movieTitle)}</span>
                            <i>${s.soldCount}/${s.seatCount} · ${s.occupancy}%</i>
                        </a>
                    </c:forEach>
                    <a class="slot-add" href="${ctx}/manager/showtimes/new?branchId=${branchId}&date=${schedule.days[vs.index].iso}" title="Thêm suất">+</a>
                </td>
            </c:forEach>
        </tr>
    </c:forEach>
    </tbody>
</table>
</div>

<c:if test="${not empty schedule.movieStats}">
<section class="card">
    <h3>Theo phim</h3>
    <table class="table">
        <thead><tr><th>Phim</th><th class="num">Số suất</th><th class="num">Vé bán</th><th>Lấp đầy</th></tr></thead>
        <tbody>
        <c:forEach var="m" items="${schedule.movieStats}">
            <tr><td>${fn:escapeXml(m.title)}</td><td class="num">${m.showtimes}</td><td class="num">${m.sold}/${m.seats}</td>
                <td><div class="bar"><span style="width:${m.occupancy}%"></span></div> <span class="small">${m.occupancy}%</span></td></tr>
        </c:forEach>
        </tbody>
    </table>
</section>
</c:if>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
