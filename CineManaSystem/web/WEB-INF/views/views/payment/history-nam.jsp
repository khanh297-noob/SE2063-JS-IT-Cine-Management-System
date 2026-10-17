<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 6.1 Payment History (khách hàng) --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Lịch Sử Thanh Toán</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo <span class="crumb">Trang Quản Lý Tài Khoản</span></div>
    <a class="btn btn-outline btn-sm" href="${ctx}/">&larr; Quay lại trang chủ</a>
</header>
<div class="container">
    <div class="card">
        <div class="card-title">
            <div>Lịch Sử Thanh Toán<div class="text-muted" style="font-weight:400;font-size:12px">Các giao dịch thanh toán vé của bạn</div></div>
            <form method="get" class="row" style="width:260px">
                <select name="status" onchange="this.form.submit()">
                    <option value="">Tất cả trạng thái</option>
                    <option value="Success" ${status == 'Success' ? 'selected' : ''}>Thành công (Success)</option>
                    <option value="Pending" ${status == 'Pending' ? 'selected' : ''}>Đang xử lý (Pending)</option>
                    <option value="Failed" ${status == 'Failed' ? 'selected' : ''}>Thất bại (Failed)</option>
                </select>
            </form>
        </div>
        <c:choose>
            <c:when test="${empty payments}">
                <p class="text-muted center">No search results.</p>
            </c:when>
            <c:otherwise>
                <table class="tbl">
                    <tr><th>Mã đơn</th><th>Phim</th><th>Rạp</th><th>Phương thức</th><th>Thời gian</th>
                        <th class="right">Số tiền</th><th>Thanh toán</th><th>Đơn hàng</th><th></th></tr>
                    <c:forEach var="p" items="${payments}">
                        <tr>
                            <td><b>${p.bookingCode}</b></td>
                            <td><c:out value="${p.movieTitle}"/><div class="text-muted">${p.ticketCount} vé</div></td>
                            <td><c:out value="${p.branchName}"/></td>
                            <td>${p.paymentMethod}</td>
                            <td><fmt:formatDate value="${p.paymentTime}" pattern="HH:mm dd/MM/yyyy"/></td>
                            <td class="right"><b><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> ₫</b></td>
                            <td><span class="st st-${p.gatewayStatus}">${p.gatewayStatus}</span></td>
                            <td><span class="st st-${p.bookingStatus}">${p.bookingStatus}</span></td>
                            <td><a class="btn btn-outline btn-sm" href="${ctx}/payment/result?bookingId=${p.bookingId}">Chi tiết</a></td>
                        </tr>
                    </c:forEach>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
