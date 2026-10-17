<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 7.1 Check cash Balance --%>
<c:set var="posTab" value="cash"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>POS CineGo - Két Tiền</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<%@include file="/WEB-INF/views/common/pos-header-nam.jspf"%>
<div class="container" style="max-width:900px">
    <div class="stats">
        <div class="stat"><div class="k">Tiền mặt phải có trong két</div>
            <div class="v text-red"><fmt:formatNumber value="${bal.expected}" pattern="#,##0"/> ₫</div></div>
        <div class="stat"><div class="k">Doanh thu ròng ca</div>
            <div class="v"><fmt:formatNumber value="${bal.netRevenue}" pattern="#,##0"/> ₫</div></div>
        <div class="stat"><div class="k">Số giao dịch</div><div class="v">${bal.transactionCount}</div></div>
    </div>
    <div class="card">
        <div class="card-title">Chi Tiết Két Tiền Ca Hiện Tại
            <span class="tag">Từ <fmt:formatDate value="${shift.startTime}" pattern="HH:mm dd/MM/yyyy"/></span></div>
        <div class="line"><span>(1) Tiền mặt đầu ca</span><b><fmt:formatNumber value="${bal.openingFloat}" pattern="#,##0"/> ₫</b></div>
        <div class="line"><span>(2) Tiền mặt thu bán vé / F&amp;B</span><b class="text-green">+ <fmt:formatNumber value="${bal.cashSales}" pattern="#,##0"/> ₫</b></div>
        <div class="line"><span>(3) Tiền mặt hoàn trả khách (${bal.refundCount} đơn hủy)</span><b class="text-red">- <fmt:formatNumber value="${bal.cashRefund}" pattern="#,##0"/> ₫</b></div>
        <div class="line total"><b>TIỀN MẶT TRONG KÉT = (1) + (2) - (3)</b><span class="amount"><fmt:formatNumber value="${bal.expected}" pattern="#,##0"/> ₫</span></div>
        <hr style="border:none;border-top:1px solid var(--border);margin:16px 0">
        <div class="line text-muted"><span>Thu qua Thẻ / QR (không vào két)</span><span><fmt:formatNumber value="${bal.cardSales}" pattern="#,##0"/> ₫</span></div>
        <div class="line text-muted"><span>Hoàn qua Thẻ / QR</span><span>- <fmt:formatNumber value="${bal.cardRefund}" pattern="#,##0"/> ₫</span></div>
        <div style="margin-top:16px;display:flex;gap:10px">
            <a class="btn btn-outline" href="${ctx}/pos/transactions">Xem giao dịch</a>
            <a class="btn btn-dark" href="${ctx}/pos/shift">Kết thúc ca &amp; đối soát</a>
        </div>
    </div>
</div>
</body>
</html>
