<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 7.1 Print ticket: vé giấy có QR (TicketCode) + hóa đơn tiền mặt. Dùng cho cả Reprint. --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>In vé ${b.bookingCode}</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <style>
        .paper { width: 320px; margin: 0 auto 16px; background: #fff; border: 1px dashed #9ca3af; padding: 16px;
                 font-family: "Courier New", monospace; font-size: 12px; }
        .paper h4 { text-align: center; margin: 0 0 4px; font-size: 16px; }
        .paper .c { text-align: center; }
        .paper hr { border: none; border-top: 1px dashed #9ca3af; margin: 8px 0; }
        .paper .l { display: flex; justify-content: space-between; }
        .paper .qr { display: flex; justify-content: center; margin: 10px 0; }
        .paper .seat { font-size: 26px; font-weight: 800; text-align: center; }
    </style>
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<div class="no-print topbar dark">
    <div class="brand"><span class="logo">POS</span> In Vé ${b.bookingCode}
        <c:if test="${isReprint}"><span class="chip">IN LẠI</span></c:if></div>
    <div>
        <button class="btn btn-primary btn-sm" onclick="window.print()">&#128424; In</button>
        <a class="btn btn-outline btn-sm" href="javascript:history.back()">Quay lại</a>
        <c:if test="${me.role == 'Ticket Cashier'}">
            <a class="btn btn-outline btn-sm" href="${ctx}/pos">Bán vé mới</a>
        </c:if>
    </div>
</div>

<div class="container">
    <%-- Vé giấy: mỗi ghế 1 vé --%>
    <c:forEach var="t" items="${b.tickets}">
        <c:if test="${t.status != 'Cancelled'}">
            <div class="paper print-ticket">
                <h4>CINEGO</h4>
                <div class="c"><c:out value="${b.branchName}"/></div>
                <hr>
                <div class="c" style="font-weight:800;font-size:14px"><c:out value="${b.movieTitle}"/> [${b.ageRating}]</div>
                <div class="c"><fmt:formatDate value="${b.showDate}" pattern="dd/MM/yyyy"/> &nbsp; <b><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/></b></div>
                <div class="c"><c:out value="${b.roomName}"/> · ${b.roomType}</div>
                <div class="seat">${t.seatLabel}</div>
                <div class="c">${t.seatType}</div>
                <div class="qr" data-code="${t.ticketCode}"></div>
                <div class="c">${t.ticketCode}</div>
                <hr>
                <div class="l"><span>Giá vé</span><b><fmt:formatNumber value="${t.finalPrice}" pattern="#,##0"/> ₫</b></div>
                <div class="l"><span>Mã đơn</span><span>${b.bookingCode}</span></div>
                <c:if test="${isReprint}"><div class="c" style="margin-top:6px">*** BẢN IN LẠI ***</div></c:if>
            </div>
        </c:if>
    </c:forEach>

    <%-- Hóa đơn --%>
    <div class="paper print-ticket">
        <h4>HÓA ĐƠN BÁN HÀNG</h4>
        <div class="c"><c:out value="${b.branchName}"/></div>
        <div class="c"><c:out value="${b.branchAddress}"/></div>
        <hr>
        <div class="l"><span>Mã đơn</span><span>${b.bookingCode}</span></div>
        <div class="l"><span>Thời gian</span><span><fmt:formatDate value="${b.bookingDate}" pattern="HH:mm dd/MM/yyyy"/></span></div>
        <div class="l"><span>Thu ngân</span><span><c:out value="${b.customerRole == 'Customer' ? 'Online' : b.customerName}"/></span></div>
        <hr>
        <c:forEach var="t" items="${b.tickets}">
            <div class="l"><span>Vé ${t.seatLabel} (${t.seatType})</span><span><fmt:formatNumber value="${t.finalPrice}" pattern="#,##0"/></span></div>
        </c:forEach>
        <c:forEach var="f" items="${b.fnbItems}">
            <div class="l"><span>${f.quantity}x <c:out value="${f.itemName}"/></span><span><fmt:formatNumber value="${f.lineTotal}" pattern="#,##0"/></span></div>
        </c:forEach>
        <hr>
        <div class="l"><span>Tạm tính</span><span><fmt:formatNumber value="${b.subtotal}" pattern="#,##0"/></span></div>
        <c:if test="${b.discount > 0}">
            <div class="l"><span>Giảm giá (<c:out value="${b.promoCode}"/>)</span><span>-<fmt:formatNumber value="${b.discount}" pattern="#,##0"/></span></div>
        </c:if>
        <div class="l" style="font-size:15px;font-weight:800"><span>TỔNG CỘNG</span><span><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</span></div>
        <div class="l"><span>Thanh toán</span><span>${b.paymentMethod}</span></div>
        <div class="l"><span>Mã GD</span><span style="font-size:10px"><c:out value="${b.transactionCode}"/></span></div>
        <hr>
        <div class="c">Cảm ơn quý khách - Hẹn gặp lại!</div>
        <c:if test="${isReprint}"><div class="c">*** BẢN IN LẠI ***</div></c:if>
    </div>
</div>

<script>
    document.querySelectorAll('.qr[data-code]').forEach(function (el) {
        new QRCode(el, {text: el.dataset.code, width: 120, height: 120, correctLevel: QRCode.CorrectLevel.M});
    });
    <c:if test="${param.auto == '1'}">
    window.addEventListener('load', function () { setTimeout(function () { window.print(); }, 500); });
    </c:if>
</script>
</body>
</html>
