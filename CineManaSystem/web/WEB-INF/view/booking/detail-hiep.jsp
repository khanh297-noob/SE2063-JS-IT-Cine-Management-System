<c:set var="pageTitle" value="Chi tiết đơn"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="b" value="${booking}"/>
<c:set var="st" value="${b.showtime}"/>
<c:if test="${b.confirmed}">
    <div class="steps"><span class="done">1. Chọn ghế</span><span class="done">2. Giữ ghế &amp; thanh toán</span><span class="active">3. Nhận vé QR</span></div>
</c:if>

<div class="page-head">
    <div>
        <h1 class="page-title">Đơn ${b.bookingCode} <span class="badge badge-${b.statusClass}">${b.displayStatus}</span></h1>
        <p class="muted">Đặt lúc ${b.bookingDateStr} · ${b.channel == 'POS' ? 'Tại quầy' : 'Online'} · ${fn:escapeXml(b.customerName)}</p>
    </div>
    <div class="actions">
        <c:if test="${me.customer}"><a class="btn btn-ghost" href="${ctx}/profile/history">← Vé của tôi</a></c:if>
    </div>
</div>

<div class="grid-2">
<section class="card">
    <c:if test="${not empty st}">
        <h2>${fn:escapeXml(st.movieTitle)} <span class="age age-${st.ageRating}">${st.ageRating}</span></h2>
        <p class="muted">${fn:escapeXml(st.branchName)} · ${fn:escapeXml(st.roomName)} (${fn:escapeXml(st.roomType)})<br>
            <b>${st.startStr} – ${st.endStr}</b> · ${st.weekday} ${st.dateStr}</p>
    </c:if>
    <dl class="kv">
        <dt>Ghế</dt><dd>${b.seatCodes}</dd>
        <dt>Tổng tiền</dt><dd><b><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b></dd>
        <dt>Thanh toán</dt><dd>${b.paymentMethod} · ${b.paymentStatus}</dd>
        <c:if test="${b.cancelled}">
            <dt>Hủy lúc</dt><dd>${b.cancelledAtStr}</dd>
            <dt>Hoàn tiền</dt><dd><fmt:formatNumber value="${b.refundAmount}" pattern="#,##0"/> ₫ <span class="muted small">(${b.paymentStatus == 'Refunded' ? 'Đã hoàn' : 'Đang xử lý'})</span></dd>
        </c:if>
    </dl>

    <c:if test="${b.confirmed}">
        <hr>
        <h3>Hủy đơn</h3>
        <c:choose>
            <c:when test="${b.cancellable}">
                <p class="muted small">Chính sách: hủy trước giờ chiếu ${cutoffHours} giờ (online) được hoàn 100% về phương thức thanh toán ban đầu. Vé mua tại quầy được hoàn tiền mặt tại quầy.</p>
                <form method="post" action="${ctx}/booking/cancel" onsubmit="return confirm('Xác nhận hủy đơn ${b.bookingCode}? Toàn bộ vé sẽ bị vô hiệu.');">
                    <input type="hidden" name="id" value="${b.bookingId}">
                    <button class="btn btn-danger">Xác nhận hủy đơn</button>
                </form>
            </c:when>
            <c:otherwise><p class="muted">Không thể hủy: ${b.cancelBlockReason}</p></c:otherwise>
        </c:choose>
    </c:if>
</section>

<section class="card">
    <h3>Vé điện tử</h3>
    <div class="tickets">
        <c:forEach var="t" items="${b.tickets}">
            <div class="ticket ${t.valid and b.confirmed ? '' : 'void'}">
                <c:choose>
                    <c:when test="${t.valid and b.confirmed}">
                        <img src="${ctx}/ticket/qr?id=${t.ticketId}&size=220" alt="QR ${t.ticketCode}" width="160" height="160">
                    </c:when>
                    <c:otherwise><div class="qr-void">${t.checkedIn ? 'ĐÃ CHECK-IN' : 'ĐÃ HỦY'}</div></c:otherwise>
                </c:choose>
                <div class="ticket-info">
                    <b class="seat-code">${t.seatCode}</b>
                    <span>${fn:escapeXml(t.seatType)}</span>
                    <span class="mono small">${t.ticketCode}</span>
                    <span><fmt:formatNumber value="${t.finalPrice}" pattern="#,##0"/> ₫</span>
                </div>
            </div>
        </c:forEach>
    </div>
    <p class="muted small">Đưa mã QR cho nhân viên soát vé tại cửa phòng chiếu (từ 30 phút trước giờ chiếu – BR-04).</p>
</section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
