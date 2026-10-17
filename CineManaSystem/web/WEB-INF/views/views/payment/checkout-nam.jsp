<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- SRS 4.3 Payment / Checkout Screen (UC-13, UC-14) --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Xác Nhận &amp; Thanh Toán</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo <span class="crumb">Xác Nhận &amp; Thanh Toán Đơn Hàng</span></div>
    <a href="javascript:history.back()">&larr; Quay lại chọn ghế</a>
</header>

<div class="container">
    <div class="hold-banner">
        Thời gian giữ ghế còn lại <span class="timer" id="holdTimer">--:--</span><br>
        Ghế của bạn đang được hệ thống giữ tạm thời. Vui lòng hoàn tất thanh toán trực tuyến trước khi thời gian đếm ngược kết thúc.
    </div>

    <div class="grid grid-2">
        <!-- ================= Cột trái ================= -->
        <div>
            <!-- Voucher (UC-12/13) -->
            <div class="card">
                <div class="card-title">&#127903; Mã Giảm Giá / Voucher <span class="tag">Tối đa 20 ký tự</span></div>
                <c:choose>
                    <c:when test="${not empty b.promoCode}">
                        <div class="row" style="align-items:center">
                            <div>
                                Đã áp dụng <b><c:out value="${b.promoCode}"/></b>
                                <span class="text-green">(-<fmt:formatNumber value="${b.discount}" pattern="#,##0"/> ₫)</span>
                            </div>
                            <form method="post" action="${ctx}/payment/checkout" class="auto">
                                <input type="hidden" name="bookingId" value="${b.bookingId}">
                                <input type="hidden" name="action" value="remove">
                                <button class="btn btn-outline btn-sm">Gỡ voucher</button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/payment/checkout">
                            <input type="hidden" name="bookingId" value="${b.bookingId}">
                            <input type="hidden" name="action" value="apply">
                            <div class="row">
                                <input type="text" name="voucherCode" maxlength="20" placeholder="NHẬP MÃ VOUCHER (VD: CINEGO50)"
                                       style="text-transform:uppercase">
                                <button class="btn btn-dark auto">Áp Dụng</button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Phương thức thanh toán (UC-14) -->
            <div class="card">
                <div class="card-title">&#128179; Phương Thức Thanh Toán Trực Tuyến</div>
                <div class="warn-text">&#9888; Bắt buộc thanh toán ngay. Không hỗ trợ giữ vé thanh toán sau tại quầy.</div>
                <%-- form="payForm": radio nằm ngoài form nhưng vẫn được gửi cùng form thanh toán --%>
                <label class="pay-option">
                    <span><input type="radio" name="paymentMethod" value="MoMo" form="payForm" checked> &nbsp;Ví điện tử MoMo</span>
                    <span class="logo-tag logo-momo">Ví MoMo</span>
                </label>
                <label class="pay-option">
                    <span><input type="radio" name="paymentMethod" value="VNPay" form="payForm"> &nbsp;Cổng thanh toán VNPay</span>
                    <span class="logo-tag logo-vnpay">VNPAY QR / ATM</span>
                </label>
                <label class="pay-option disabled" title="Sắp ra mắt">
                    <span><input type="radio" disabled> &nbsp;Ví điện tử ZaloPay <small class="text-muted">(sắp ra mắt)</small></span>
                    <span class="logo-tag logo-zalo">ZaloPay</span>
                </label>
            </div>
        </div>

        <!-- ================= Cột phải: Tóm tắt đơn ================= -->
        <div class="card">
            <div class="card-title">Tóm Tắt Đơn Hàng <span class="tag">${b.bookingCode}</span></div>
            <div class="movie-box">
                <div class="title"><c:out value="${b.movieTitle}"/> <span class="age">${b.ageRating}</span></div>
                <div>Rạp: <b><c:out value="${b.branchName}"/> (<c:out value="${b.roomName}"/>)</b></div>
                <div>Suất chiếu: <b><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/> - <fmt:formatDate value="${b.showDate}" pattern="EEEE, dd/MM/yyyy"/></b></div>
                <div>Định dạng: <b class="text-red">${b.roomType}</b></div>
            </div>

            <div class="line">
                <div><b>Ghế đã chọn (${b.ticketCount}x):</b><small>Ghế ${b.seatLabels}</small></div>
                <b><fmt:formatNumber value="${b.ticketTotal}" pattern="#,##0"/> ₫</b>
            </div>
            <c:if test="${not empty b.fnbItems}">
                <div class="line">
                    <div><b>Combo Bắp Nước (F&amp;B):</b>
                        <c:forEach var="f" items="${b.fnbItems}">
                            <small>${f.quantity}x <c:out value="${f.itemName}"/></small>
                        </c:forEach>
                    </div>
                    <b><fmt:formatNumber value="${b.fnbTotal}" pattern="#,##0"/> ₫</b>
                </div>
            </c:if>
            <div class="line text-muted"><span>Tạm tính (Subtotal)</span><span><fmt:formatNumber value="${b.subtotal}" pattern="#,##0"/> ₫</span></div>
            <div class="line text-muted"><span>Giảm giá Voucher</span><span class="text-green">- <fmt:formatNumber value="${b.discount}" pattern="#,##0"/> ₫</span></div>
            <div class="line total">
                <div><b>TỔNG TIỀN THANH TOÁN</b><small>(Amount due)</small></div>
                <span class="amount"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</span>
            </div>

            <form id="payForm" method="post" action="${ctx}/api/v1/payment/create" style="margin-top:16px">
                <input type="hidden" name="bookingId" value="${b.bookingId}">
                <button class="btn btn-primary btn-block" id="btnPay">&#128274; Xác Nhận &amp; Thanh Toán Online</button>
            </form>
        </div>
    </div>
</div>

<script>
    // Đếm ngược thời gian giữ ghế - hết giờ thì khóa nút thanh toán
    var left = ${secondsLeft};
    var timerEl = document.getElementById('holdTimer');
    function tick() {
        if (left <= 0) {
            timerEl.textContent = '00:00';
            document.getElementById('btnPay').disabled = true;
            return;
        }
        var m = Math.floor(left / 60), s = left % 60;
        timerEl.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
        left--;
        setTimeout(tick, 1000);
    }
    tick();
</script>
</body>
</html>
