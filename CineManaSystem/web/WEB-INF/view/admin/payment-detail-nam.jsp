<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/view/common/taglibs-nam.jspf"%>
<%-- 6.1 Manage Payment - chi tiết + đối soát giao dịch Pending --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Chi Tiết Thanh Toán</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/view/common/flash-nam.jspf"%>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo <span class="crumb">Chi Tiết Thanh Toán #${p.paymentId}</span></div>
    <a href="${ctx}/admin/payments">&larr; Danh sách thanh toán</a>
</header>
<div class="container">
    <div class="grid grid-2">
        <div class="card">
            <div class="card-title">Giao dịch <span class="st st-${p.gatewayStatus}">${p.gatewayStatus}</span></div>
            <div class="info-grid">
                <div><small>Payment ID</small>${p.paymentId}</div>
                <div><small>Mã đơn</small><b>${p.bookingCode}</b> <span class="st st-${p.bookingStatus}">${p.bookingStatus}</span></div>
                <div><small>Phương thức</small>${p.paymentMethod}</div>
                <div><small>Số tiền</small><b class="text-red"><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> ₫</b></div>
                <div><small>Mã giao dịch</small><span style="word-break:break-all"><c:out value="${p.transactionCode}"/></span></div>
                <div><small>Thời gian thanh toán</small><fmt:formatDate value="${p.paymentTime}" pattern="HH:mm:ss dd/MM/yyyy"/></div>
                <div><small>Khách / Người tạo</small><c:out value="${p.customerName}"/> (<c:out value="${p.customerRole}"/>)</div>
                <div><small>Email</small><c:out value="${p.customerEmail}"/></div>
                <div><small>Ngày đặt</small><fmt:formatDate value="${p.bookingDate}" pattern="HH:mm:ss dd/MM/yyyy"/></div>
            </div>

            <c:if test="${p.gatewayStatus == 'Pending'}">
                <hr style="border:none;border-top:1px solid var(--border);margin:18px 0">
                <div class="alert alert-info">Giao dịch đang chờ kết quả từ cổng. Đối chiếu trên trang quản trị VNPay / MoMo rồi cập nhật thủ công nếu cần.</div>
                <form method="post" action="${ctx}/admin/payments" class="row">
                    <input type="hidden" name="id" value="${p.paymentId}">
                    <button class="btn btn-green" name="newStatus" value="Success"
                            onclick="return confirm('Xác nhận giao dịch THÀNH CÔNG? Đơn sẽ được Confirmed.')">&#10003; Đánh dấu Success</button>
                    <button class="btn btn-outline" name="newStatus" value="Failed"
                            onclick="return confirm('Xác nhận giao dịch THẤT BẠI? Đơn bị hủy, ghế được nhả.')">Đánh dấu Failed</button>
                </form>
            </c:if>
        </div>

        <div class="card">
            <div class="card-title">Đơn hàng</div>
            <c:if test="${not empty b}">
                <div class="movie-box">
                    <div class="title"><c:out value="${b.movieTitle}"/></div>
                    <div>Rạp: <b><c:out value="${b.branchName}"/> · <c:out value="${b.roomName}"/> (${b.roomType})</b></div>
                    <div>Suất: <b><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/> <fmt:formatDate value="${b.showDate}" pattern="dd/MM/yyyy"/></b></div>
                </div>
                <c:forEach var="t" items="${b.tickets}">
                    <div class="line"><span>Ghế ${t.seatLabel} (${t.seatType}) <span class="st st-${t.status}">${t.status}</span></span>
                        <span><fmt:formatNumber value="${t.finalPrice}" pattern="#,##0"/> ₫</span></div>
                </c:forEach>
                <c:forEach var="f" items="${b.fnbItems}">
                    <div class="line"><span>${f.quantity}x <c:out value="${f.itemName}"/></span>
                        <span><fmt:formatNumber value="${f.lineTotal}" pattern="#,##0"/> ₫</span></div>
                </c:forEach>
                <div class="line text-muted"><span>Voucher <c:out value="${b.promoCode}"/></span><span>- <fmt:formatNumber value="${b.discount}" pattern="#,##0"/> ₫</span></div>
                <div class="line total"><b>TỔNG</b><span class="amount"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</span></div>
            </c:if>
        </div>
    </div>
</div>
</body>
</html>
