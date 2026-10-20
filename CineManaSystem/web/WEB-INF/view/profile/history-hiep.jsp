<c:set var="pageTitle" value="Vé của tôi"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="page-head">
    <h1 class="page-title">Lịch sử đặt vé</h1>
    <span class="muted">${totalCount} đơn</span>
</div>
<div class="day-tabs">
    <a class="day-tab ${filter == 'all' ? 'active' : ''}" href="?filter=all">Tất cả</a>
    <a class="day-tab ${filter == 'upcoming' ? 'active' : ''}" href="?filter=upcoming">Sắp chiếu</a>
    <a class="day-tab ${filter == 'past' ? 'active' : ''}" href="?filter=past">Đã chiếu</a>
    <a class="day-tab ${filter == 'cancelled' ? 'active' : ''}" href="?filter=cancelled">Đã hủy</a>
</div>
<c:choose>
    <c:when test="${empty bookings}">
        <div class="empty">Không có đơn nào. <a href="${ctx}/home">Đặt vé ngay →</a></div>
    </c:when>
    <c:otherwise>
        <div class="history">
            <c:forEach var="b" items="${bookings}">
                <a class="history-item" href="${ctx}/booking/detail?id=${b.bookingId}">
                    <div class="h-date">
                        <b>${b.showtime.startStr}</b>
                        <span>${b.showtime.weekday} ${b.showtime.dateStr}</span>
                    </div>
                    <div class="h-main">
                        <b>${fn:escapeXml(b.showtime.movieTitle)}</b>
                        <span class="muted small">${fn:escapeXml(b.showtime.branchName)} · ${fn:escapeXml(b.showtime.roomName)} · Ghế ${b.seatCodes}</span>
                        <span class="muted small mono">${b.bookingCode} · đặt ${b.bookingDateStr}</span>
                    </div>
                    <div class="h-side">
                        <span class="badge badge-${b.statusClass}">${b.displayStatus}</span>
                        <b><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b>
                        <c:if test="${b.upcoming}"><span class="small accent">Xem QR →</span></c:if>
                    </div>
                </a>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
