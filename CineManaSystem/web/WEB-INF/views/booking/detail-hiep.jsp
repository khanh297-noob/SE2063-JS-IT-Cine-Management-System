<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageTitle" value="Đơn ${b.bookingCode}"/>
<c:set var="extraCss" value="booking-hiep"/>
<c:set var="customerNav" value="tickets"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <a class="back" href="${ctx}/customer/bookings">&larr; Vé của tôi</a>
    <c:if test="${b.status == 'Confirmed'}">
        <ol class="bk-steps">
            <li class="done">1. Chọn phim &amp; suất chiếu</li>
            <li class="done">2. Chọn ghế</li>
            <li class="done">3. Đồ ăn &amp; nước</li>
            <li class="done">4. Thanh toán VNPay</li>
            <li class="active">5. Nhận vé QR</li>
        </ol>
    </c:if>

    <div class="page-head">
        <div>
            <h1 class="page-title">Đơn ${b.bookingCode}
                <c:choose>
                    <c:when test="${b.status == 'Pending'}"><span class="bk-badge Pending">Chờ thanh toán</span></c:when>
                    <c:when test="${b.status == 'Cancelled'}"><span class="bk-badge Cancelled">Đã hủy</span></c:when>
                    <c:otherwise><span class="bk-badge Confirmed">Đã thanh toán</span></c:otherwise>
                </c:choose>
            </h1>
            <p class="page-sub">Đặt lúc <fmt:formatDate value="${b.bookingDate}" pattern="HH:mm dd/MM/yyyy"/>
                · ${b.channel == 'Counter' ? 'Tại quầy' : 'Online'} · <c:out value="${b.customerName}"/></p>
        </div>
        <c:if test="${b.status == 'Pending'}">
            <a class="btn btn-primary" href="${ctx}/payment/checkout?bookingId=${b.bookingId}">Tiếp tục thanh toán VNPay</a>
        </c:if>
    </div>

    <div class="bk-grid">
        <section class="bk-panel">
            <h2><c:out value="${b.movieTitle}"/> <span class="bk-age age-${b.ageRating}">${b.ageRating}</span></h2>
            <p class="bk-muted"><c:out value="${b.branchName}"/> · <c:out value="${b.roomName}"/> (<c:out value="${b.roomType}"/>)<br>
                <c:out value="${b.branchAddress}"/><br>
                <b><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/></b> · <fmt:formatDate value="${b.showDate}" pattern="EEEE, dd/MM/yyyy"/></p>
            <dl class="bk-kv">
                <dt>Ghế</dt><dd>${b.seatLabels}</dd>
                <dt>Tiền vé</dt><dd><fmt:formatNumber value="${b.ticketTotal}" pattern="#,##0"/> ₫</dd>
                <c:if test="${not empty b.fnbItems}">
                    <dt>Đồ ăn &amp; nước</dt>
                    <dd><c:forEach var="f" items="${b.fnbItems}" varStatus="vs">${vs.index > 0 ? '<br>' : ''}${f.quantity} × <c:out value="${f.itemName}"/></c:forEach></dd>
                </c:if>
                <c:if test="${not empty b.promoCode}">
                    <dt>Voucher</dt><dd><span class="bk-mono"><c:out value="${b.promoCode}"/></span> · -<fmt:formatNumber value="${b.discount}" pattern="#,##0"/> ₫</dd>
                </c:if>
                <dt>Tổng tiền</dt><dd><b><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b></dd>
                <dt>Thanh toán</dt><dd><c:out value="${empty b.paymentMethod ? 'Chưa thanh toán' : b.paymentMethod}"/> <c:if test="${not empty b.paymentStatus}">· ${b.paymentStatus}</c:if></dd>
                <c:if test="${b.status == 'Cancelled'}">
                    <c:if test="${not empty b.cancelledAt}"><dt>Hủy lúc</dt><dd><fmt:formatDate value="${b.cancelledAt}" pattern="HH:mm dd/MM/yyyy"/></dd></c:if>
                    <c:if test="${not empty b.refundAmount}">
                        <dt>Hoàn tiền</dt><dd><fmt:formatNumber value="${b.refundAmount}" pattern="#,##0"/> ₫
                            <span class="bk-muted bk-small">(${b.paymentStatus == 'Refunded' ? 'Đã hoàn' : 'Đang xử lý'})</span></dd>
                    </c:if>
                </c:if>
            </dl>

            <c:if test="${b.status == 'Confirmed' and b.userId == sessionScope.User.userId}">
                <h3 style="margin-top: 22px">Hủy đơn</h3>
                <c:choose>
                    <c:when test="${empty cancelBlockReason}">
                        <p class="bk-muted bk-small">Hủy trước giờ chiếu ${cutoffHours} giờ được hoàn 100% về phương thức thanh toán ban đầu.</p>
                        <form method="post" action="${ctx}/booking/cancel" onsubmit="return confirm('Xác nhận hủy đơn ${b.bookingCode}? Toàn bộ vé sẽ bị vô hiệu.');">
                            <input type="hidden" name="id" value="${b.bookingId}">
                            <button class="btn btn-danger">Hủy đơn &amp; hoàn tiền</button>
                        </form>
                    </c:when>
                    <c:otherwise><p class="bk-muted">Không thể hủy: <c:out value="${cancelBlockReason}"/></p></c:otherwise>
                </c:choose>
            </c:if>
        </section>

        <section class="bk-panel">
            <h3>Vé điện tử</h3>
            <div class="bk-tickets">
                <c:forEach var="t" items="${b.tickets}">
                    <c:set var="usable" value="${t.status == 'Valid' and b.status == 'Confirmed'}"/>
                    <div class="bk-ticket ${usable ? '' : 'void'}">
                        <c:choose>
                            <c:when test="${usable}">
                                <img src="${ctx}/ticket/qr?id=${t.ticketId}&size=220" alt="QR ${t.ticketCode}" width="150" height="150">
                            </c:when>
                            <c:otherwise>
                                <div class="bk-qr-void">${t.status == 'Checked-In' ? 'ĐÃ CHECK-IN' : (b.status == 'Pending' ? 'CHỜ THANH TOÁN' : 'ĐÃ HỦY')}</div>
                            </c:otherwise>
                        </c:choose>
                        <div class="bk-ticket-info">
                            <b class="bk-seat-code">${t.seatLabel}</b>
                            <span><c:out value="${t.seatType}"/></span>
                            <span class="bk-mono bk-small">${t.ticketCode}</span>
                            <span><fmt:formatNumber value="${t.finalPrice}" pattern="#,##0"/> ₫</span>
                        </div>
                    </div>
                </c:forEach>
            </div>
            <p class="bk-muted bk-small">Đưa mã QR cho nhân viên soát vé tại cửa phòng chiếu.</p>
        </section>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
