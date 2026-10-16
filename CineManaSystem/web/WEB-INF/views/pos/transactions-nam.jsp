<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 7.1 Manage Transactions --%>
<c:set var="posTab" value="tx"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>POS CineGo - Giao Dịch</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<%@include file="/WEB-INF/views/common/pos-header-nam.jspf"%>
<div class="container">
    <div class="card">
        <form method="get" class="row">
            <div><label>Ngày</label><input type="date" name="date" value="${date}"></div>
            <div><label>Trạng thái</label>
                <select name="status">
                    <option value="">Tất cả</option>
                    <option value="Confirmed" ${status == 'Confirmed' ? 'selected' : ''}>Confirmed</option>
                    <option value="Cancelled" ${status == 'Cancelled' ? 'selected' : ''}>Cancelled</option>
                </select></div>
            <div class="auto"><button class="btn btn-primary">Lọc</button></div>
            <div class="auto" style="text-align:right">
                <div class="text-muted" style="font-size:12px">Tổng đơn hợp lệ</div>
                <b class="text-red" style="font-size:20px"><fmt:formatNumber value="${sum}" pattern="#,##0"/> ₫</b>
            </div>
        </form>
    </div>
    <div class="card">
        <c:choose>
            <c:when test="${empty list}"><p class="text-muted center">No search results.</p></c:when>
            <c:otherwise>
                <table class="tbl">
                    <tr><th>Mã đơn</th><th>Thời gian</th><th>Phim · Suất</th><th>Ghế</th><th>F&amp;B</th>
                        <th>Thanh toán</th><th class="right">Tổng tiền</th><th>Trạng thái</th><th></th></tr>
                    <c:forEach var="b" items="${list}">
                        <tr>
                            <td><b>${b.bookingCode}</b></td>
                            <td><fmt:formatDate value="${b.bookingDate}" pattern="HH:mm:ss"/></td>
                            <td><c:out value="${b.movieTitle}"/><div class="text-muted"><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/> · <c:out value="${b.roomName}"/></div></td>
                            <td>${b.seatLabels}</td>
                            <td>${fn:length(b.fnbItems)} món</td>
                            <td>${b.paymentMethod}</td>
                            <td class="right"><b><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b></td>
                            <td><span class="st st-${b.status}">${b.status}</span></td>
                            <td><a class="btn btn-outline btn-sm" href="${ctx}/pos/transactions?action=detail&id=${b.bookingId}">Chi tiết</a></td>
                        </tr>
                    </c:forEach>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
