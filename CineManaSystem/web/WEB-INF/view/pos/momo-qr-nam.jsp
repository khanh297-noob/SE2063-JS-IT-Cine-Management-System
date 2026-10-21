<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/view/common/taglibs-nam.jspf"%>
<%-- Thanh toán MoMo QR tại quầy: QR = link thanh toán MoMo sandbox.
     Trang tự tải lại mỗi 5 giây, Servlet hỏi MoMo khách đã trả chưa. --%>
<c:set var="posTab" value="sell"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="refresh" content="5">
    <title>POS CineGo - Thanh toán MoMo ${b.bookingCode}</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
    <style>
        .momo-box { max-width: 520px; margin: 0 auto; text-align: center; }
        .momo-title { color: #a50064; font-weight: 800; font-size: 20px; margin-bottom: 4px; }
        .momo-amount { font-size: 34px; font-weight: 800; color: #a50064; margin: 6px 0 14px; }
        #qr { display: inline-block; padding: 12px; border: 6px solid #a50064; border-radius: 14px; background: #fff; }
        .waiting { margin-top: 14px; color: var(--muted); }
    </style>
</head>
<body>
<%@include file="/WEB-INF/view/common/flash-nam.jspf"%>
<%@include file="/WEB-INF/view/common/pos-header-nam.jspf"%>

<div class="container">
    <div class="card momo-box">
        <div class="momo-title">Thanh toán bằng MoMo</div>
        <div class="text-muted">Đơn ${b.bookingCode} · <c:out value="${b.movieTitle}"/> · Ghế ${b.seatLabels}</div>
        <div class="momo-amount"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</div>

        <c:choose>
            <c:when test="${not empty payUrl}">
                <div id="qr"></div>
                <div class="alert alert-info" style="text-align:left;margin-top:16px">
                    Mời khách dùng <b>camera điện thoại</b> quét mã QR → trang thanh toán MoMo mở ra →
                    chọn <b>Thẻ ATM</b> và thanh toán. Màn hình này tự chuyển sang in vé khi khách trả xong.
                </div>
                <%-- Để test trên chính máy tính: mở link thanh toán ở tab mới --%>
                <a class="btn btn-outline btn-sm" href="${payUrl}" target="_blank">Mở trang thanh toán trên máy này (test)</a>
            </c:when>
            <c:otherwise>
                <div class="alert alert-error">Không còn link thanh toán (phiên làm việc đã đổi). Hãy hủy và tạo lại đơn.</div>
            </c:otherwise>
        </c:choose>

        <div class="waiting">&#8987; Đang chờ khách thanh toán... Ghế được giữ thêm <b id="t">--:--</b> · tự kiểm tra mỗi 5 giây</div>

        <form method="post" action="${ctx}/pos/momo-qr" style="margin-top:16px"
              onsubmit="return confirm('Hủy thanh toán MoMo và nhả ghế?')">
            <input type="hidden" name="bookingId" value="${b.bookingId}">
            <button class="btn btn-dark">Hủy (khách đổi ý / trả tiền mặt)</button>
        </form>
    </div>
</div>

<script>
    // Vẽ mã QR từ link thanh toán MoMo
    <c:if test="${not empty payUrl}">
    new QRCode(document.getElementById('qr'), {text: '${payUrl}', width: 240, height: 240, correctLevel: QRCode.CorrectLevel.M});
    </c:if>
    // Đếm ngược thời gian giữ ghế
    var left = ${secondsLeft}, el = document.getElementById('t');
    (function tick() {
        var m = Math.floor(left / 60), s = left % 60;
        el.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
        if (left-- > 0) setTimeout(tick, 1000);
    })();
</script>
</body>
</html>
