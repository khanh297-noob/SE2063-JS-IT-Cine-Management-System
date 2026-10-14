<c:set var="pageTitle" value="Tra cứu đơn"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="page-head">
    <div>
        <h1 class="page-title">Tra cứu / Hủy đơn tại quầy</h1>
        <p class="muted">${fn:escapeXml(branch.name)}</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/pos">← Bán vé</a>
</div>
<form method="get" class="filter-bar">
    <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Mã đơn (BK000001), SĐT hoặc email khách" class="grow" autofocus>
    <button class="btn btn-primary">Tìm</button>
</form>
<c:if test="${not empty error}"><div class="alert alert-error">${fn:escapeXml(error)}</div></c:if>
<c:if test="${not empty q}">
    <c:choose>
        <c:when test="${empty results}"><div class="empty">Không tìm thấy đơn nào. (MSG01)</div></c:when>
        <c:otherwise>
            <div class="table-wrap">
            <table class="table">
                <thead><tr><th>Mã đơn</th><th>Khách</th><th>Suất chiếu</th><th>Ghế</th><th class="num">Tổng</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="b" items="${results}">
                    <tr>
                        <td class="mono">${b.bookingCode}<div class="muted small">${b.channel}</div></td>
                        <td>${fn:escapeXml(b.customerName)}<div class="muted small">${b.customerPhone}</div></td>
                        <td>${fn:escapeXml(b.showtime.movieTitle)}<div class="muted small">${b.showtime.startStr} ${b.showtime.dateStr} · ${fn:escapeXml(b.showtime.roomName)}</div></td>
                        <td>${b.seatCodes}</td>
                        <td class="num"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</td>
                        <td><span class="badge badge-${b.statusClass}">${b.displayStatus}</span></td>
                        <td class="row-actions">
                            <a class="btn btn-ghost btn-sm" href="${ctx}/booking/detail?id=${b.bookingId}">Xem / In</a>
                            <c:if test="${b.cancellable}">
                                <form method="post" action="${ctx}/booking/cancel" class="inline" onsubmit="return confirm('Hủy đơn ${b.bookingCode} và hoàn tiền cho khách?');">
                                    <input type="hidden" name="id" value="${b.bookingId}">
                                    <button class="btn btn-danger btn-sm">Hủy</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
