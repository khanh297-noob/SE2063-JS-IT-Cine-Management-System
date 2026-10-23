<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/view/common/taglibs-nam.jspf"%>
<%-- 7.1 Manage Transactions - chi tiết, in lại, hủy & hoàn tiền tại quầy (UC-22) --%>
<c:set var="posTab" value="tx"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>POS CineGo - ${b.bookingCode}</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/view/common/flash-nam.jspf"%>
<%@include file="/WEB-INF/view/common/pos-header-nam.jspf"%>
<div class="container">
    <a href="${ctx}/pos/transactions">&larr; Danh sách giao dịch</a>
    <div class="grid grid-2" style="margin-top:12px">
        <div class="card">
            <div class="card-title">Đơn ${b.bookingCode} <span class="st st-${b.status}">${b.status}</span></div>
            <div class="movie-box">
                <div class="title"><c:out value="${b.movieTitle}"/> <span class="age">${b.ageRating}</span></div>
                <div>Rạp: <b><c:out value="${b.branchName}"/> · <c:out value="${b.roomName}"/> (${b.roomType})</b></div>
                <div>Suất chiếu: <b><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/> - <fmt:formatDate value="${b.showDate}" pattern="dd/MM/yyyy"/></b></div>
                <div>Tạo lúc: <b><fmt:formatDate value="${b.bookingDate}" pattern="HH:mm:ss dd/MM/yyyy"/></b></div>
            </div>
            <table class="tbl">
                <tr><th>Ghế</th><th>Mã vé</th><th>Trạng thái</th><th class="right">Giá</th></tr>
                <c:forEach var="t" items="${b.tickets}">
                    <tr><td><b>${t.seatLabel}</b> <span class="text-muted">${t.seatType}</span></td><td>${t.ticketCode}</td>
                        <td><span class="st st-${t.status}">${t.status}</span></td>
                        <td class="right"><fmt:formatNumber value="${t.finalPrice}" pattern="#,##0"/> ₫</td></tr>
                </c:forEach>
                <c:forEach var="f" items="${b.fnbItems}">
                    <tr><td colspan="3">${f.quantity}x <c:out value="${f.itemName}"/></td>
                        <td class="right"><fmt:formatNumber value="${f.lineTotal}" pattern="#,##0"/> ₫</td></tr>
                </c:forEach>
            </table>
            <div class="line text-muted"><span>Giảm giá <c:out value="${b.promoCode}"/></span><span>- <fmt:formatNumber value="${b.discount}" pattern="#,##0"/> ₫</span></div>
            <div class="line total"><div><b>TỔNG</b><small>${b.paymentMethod} · <c:out value="${b.transactionCode}"/></small></div>
                <span class="amount"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</span></div>
            <c:if test="${b.status == 'Confirmed' && !b.allCheckedIn}">
                <a class="btn btn-dark" style="margin-top:12px" href="${ctx}/pos/print?bookingId=${b.bookingId}&reprint=1">&#128424; In lại vé (Reprint)</a>
            </c:if>
        </div>

        <div class="card">
            <div class="card-title">Hủy Vé &amp; Hoàn Tiền <span class="tag">UC-22</span></div>
            <c:choose>
                <c:when test="${b.status == 'Confirmed' && !b.anyCheckedIn}">
                    <div class="alert alert-info">
                        Chỉ hủy được khi chưa vé nào check-in và trước giờ chiếu ít nhất 60 phút.
                        Cần <b>Quản lý chi nhánh</b> xác nhận. Hoàn
                        <b><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b> bằng ${b.paymentMethod}.
                    </div>
                    <form method="post" action="${ctx}/pos/transactions" onsubmit="return confirm('Xác nhận hủy đơn và hoàn tiền cho khách?')">
                        <input type="hidden" name="action" value="cancel">
                        <input type="hidden" name="id" value="${b.bookingId}">
                        <div class="field"><label>Lý do hủy *</label><input type="text" name="reason" maxlength="60" required></div>
                        <div class="field"><label>Email quản lý *</label><input type="email" name="supervisorEmail" required></div>
                        <div class="field"><label>Mật khẩu quản lý *</label><input type="password" name="supervisorPassword" required></div>
                        <button class="btn btn-primary btn-block">Xác Nhận Hủy &amp; Hoàn Tiền</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <p class="text-muted">This booking can no longer be cancelled or refunded.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
</body>
</html>
