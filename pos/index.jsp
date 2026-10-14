<c:set var="pageTitle" value="Bán vé tại quầy"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="page-head">
    <div>
        <h1 class="page-title">Bán vé tại quầy</h1>
        <p class="muted">${fn:escapeXml(branch.name)} – chỉ hiển thị suất chiếu của chi nhánh bạn</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/pos/bookings">Tra cứu / Hủy đơn</a>
</div>
<div class="day-tabs">
    <c:forEach var="d" items="${days}">
        <a class="day-tab ${d.iso == selectedDate ? 'active' : ''}" href="${ctx}/pos?date=${d.iso}">${d.today ? 'Hôm nay' : d.label}</a>
    </c:forEach>
</div>
<c:choose>
    <c:when test="${empty showtimes}"><div class="empty">Không có suất chiếu. (MSG01)</div></c:when>
    <c:otherwise>
        <div class="pos-grid">
            <c:forEach var="s" items="${showtimes}">
                <c:choose>
                    <c:when test="${s.bookable}">
                        <a class="pos-tile" href="${ctx}/booking/seats?showtimeId=${s.showtimeId}">
                            <b class="time">${s.startStr}</b>
                            <span class="title">${fn:escapeXml(s.movieTitle)} <span class="age age-${s.ageRating}">${s.ageRating}</span></span>
                            <span class="muted small">${fn:escapeXml(s.roomName)} · ${s.roomType}</span>
                            <span class="small">Còn <b>${s.availableCount}</b>/${s.seatCount} ghế</span>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <div class="pos-tile disabled">
                            <b class="time">${s.startStr}</b>
                            <span class="title">${fn:escapeXml(s.movieTitle)}</span>
                            <span class="muted small">${fn:escapeXml(s.roomName)} · ${s.displayStatus}</span>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
