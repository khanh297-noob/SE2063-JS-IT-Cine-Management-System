<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/view/common/taglibs-nam.jspf"%>
<%-- Payment Status: kết quả thanh toán + E-ticket QR (UC-15) --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Kết Quả Thanh Toán</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
    <%-- Đang chờ xác nhận -> trình duyệt tự tải lại trang mỗi 5 giây để cập nhật trạng thái --%>
    <c:if test="${state == 'PENDING'}"><meta http-equiv="refresh" content="5"></c:if>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
</head>
<body>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo <span class="crumb">Kết Quả Thanh Toán</span></div>
    <a href="${ctx}/payment/history">Lịch sử thanh toán &rarr;</a>
</header>

<div class="container" style="max-width:820px">
    <c:choose>
        <c:when test="${state == 'SUCCESS'}">
            <div class="clearance ok" style="margin-bottom:18px">
                <div class="icon">&#10003;</div>
                <span class="pill">THANH TOÁN THÀNH CÔNG</span>
                <div class="big">Payment completed successfully.</div>
                <p class="text-muted">Vé điện tử đã được gửi tới email <b><c:out value="${b.customerEmail}"/></b>. Đưa mã QR cho nhân viên soát vé tại cửa phòng chiếu.</p>
            </div>
        </c:when>
        <c:when test="${state == 'PENDING'}">
            <div class="clearance" style="margin-bottom:18px">
                <div class="icon">&#8987;</div>
                <span class="pill">ĐANG XỬ LÝ</span>
                <div class="big">Đang chờ xác nhận thanh toán...</div>
                <p class="text-muted">Trang tự cập nhật mỗi 5 giây. Vui lòng không đóng trình duyệt.</p>
            </div>
        </c:when>
        <c:when test="${state == 'LATE'}">
            <div class="clearance no" style="margin-bottom:18px">
                <div class="icon">!</div>
                <span class="pill">CẦN HỖ TRỢ</span>
                <div class="big">Thanh toán đến sau khi hết thời gian giữ ghế</div>
                <p class="text-muted">Giao dịch đã được ghi nhận, bộ phận hỗ trợ sẽ hoàn tiền cho bạn. Mã đơn: <b>${b.bookingCode}</b></p>
            </div>
        </c:when>
        <c:when test="${state == 'FAILED'}">
            <div class="clearance no" style="margin-bottom:18px">
                <div class="icon">&#10005;</div>
                <span class="pill">THẤT BẠI</span>
                <div class="big">Payment failed. Please try again.</div>
                <p class="text-muted">Ghế giữ tạm đã được nhả. Vui lòng đặt vé lại.</p>
                <a class="btn btn-primary" href="${ctx}/">Đặt vé lại</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="clearance no" style="margin-bottom:18px">
                <div class="icon">&#8987;</div>
                <span class="pill">HẾT HẠN</span>
                <div class="big">Payment timed out. Your reserved seats have been released.</div>
                <a class="btn btn-primary" href="${ctx}/" style="margin-top:10px">Đặt vé lại</a>
            </div>
        </c:otherwise>
    </c:choose>

    <div class="card">
        <div class="card-title">Thông Tin Đơn Hàng <span class="st st-${b.status}">${b.status}</span></div>
        <div class="info-grid" style="margin-bottom:16px">
            <div><small>Mã đơn hàng</small><b>${b.bookingCode}</b></div>
            <div><small>Phim</small><b><c:out value="${b.movieTitle}"/></b></div>
            <div><small>Suất chiếu &amp; Phòng</small><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/> - <fmt:formatDate value="${b.showDate}" pattern="dd/MM/yyyy"/> · <c:out value="${b.roomName}"/></div>
            <div><small>Rạp</small><c:out value="${b.branchName}"/></div>
            <div><small>Phương thức</small>${b.paymentMethod} <span class="st st-${b.paymentStatus}">${b.paymentStatus}</span></div>
            <div><small>Số tiền</small><b class="text-red"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b></div>
            <div><small>Mã giao dịch</small><c:out value="${b.transactionCode}"/></div>
            <div><small>Thời gian</small><fmt:formatDate value="${b.paymentTime}" pattern="HH:mm:ss dd/MM/yyyy"/></div>
        </div>

        <c:if test="${state == 'SUCCESS'}">
            <h3 style="font-size:15px">Vé điện tử (E-Ticket)</h3>
            <c:forEach var="t" items="${b.tickets}">
                <div class="eticket">
                    <div class="qr" data-code="${t.ticketCode}"></div>
                    <div>
                        <div style="font-weight:800"><c:out value="${b.movieTitle}"/></div>
                        <div class="text-muted">Ghế <b class="text-red">${t.seatLabel}</b> (${t.seatType}) · ${b.roomType}</div>
                        <div class="text-muted">Mã vé: <b>${t.ticketCode}</b></div>
                        <div><span class="st st-${t.status}">${t.status}</span></div>
                    </div>
                </div>
            </c:forEach>
        </c:if>
    </div>
</div>

<script>
    document.querySelectorAll('.qr[data-code]').forEach(function (el) {
        new QRCode(el, {text: el.dataset.code, width: 118, height: 118, correctLevel: QRCode.CorrectLevel.M});
    });
</script>
</body>
</html>
