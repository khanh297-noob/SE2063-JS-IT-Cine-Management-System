<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 6.1 Manage Payment (Administrator / Branch Management) --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Quản Lý Thanh Toán</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css"></head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo <span class="crumb">Quản Lý Thanh Toán (Manage Payment)</span></div>
    <div class="user"><b><c:out value="${me.fullName}"/></b><br><c:out value="${me.role}"/></div>
</header>
<div class="container">
    <div class="stats">
        <div class="stat"><div class="k">Số giao dịch</div><div class="v">${totalRecords}</div></div>
        <div class="stat"><div class="k">Tổng tiền thành công</div><div class="v text-red"><fmt:formatNumber value="${totalSuccess}" pattern="#,##0"/> ₫</div></div>
        <div class="stat"><div class="k">Khoảng thời gian</div><div class="v" style="font-size:15px">${from} → ${to}</div></div>
    </div>

    <div class="card">
        <form method="get" class="row" style="flex-wrap:wrap">
            <div style="flex:2"><label>Tìm kiếm</label>
                <input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Tên, email, mã đơn CG-..., mã giao dịch"></div>
            <div><label>Phương thức</label>
                <select name="method">
                    <option value="">Tất cả</option>
                    <c:forEach var="m" items="VNPay,MoMo,Cash,Credit Card">
                        <option value="${m}" ${method == m ? 'selected' : ''}>${m}</option>
                    </c:forEach>
                </select></div>
            <div><label>Trạng thái</label>
                <select name="status">
                    <option value="">Tất cả</option>
                    <c:forEach var="s" items="Success,Pending,Failed">
                        <option value="${s}" ${status == s ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select></div>
            <div><label>Rạp</label>
                <select name="branchId">
                    <option value="0">Tất cả rạp</option>
                    <c:forEach var="br" items="${branches}">
                        <option value="${br.branchId}" ${branchId == br.branchId ? 'selected' : ''}><c:out value="${br.name}"/></option>
                    </c:forEach>
                </select></div>
            <div><label>Từ ngày</label><input type="date" name="from" value="${from}"></div>
            <div><label>Đến ngày</label><input type="date" name="to" value="${to}"></div>
            <div class="auto"><button class="btn btn-primary">Lọc</button></div>
        </form>
        <c:if test="${not empty dateError}"><div class="field-error">${dateError}</div></c:if>    </div>

    <div class="card">
        <c:choose>
            <c:when test="${empty payments}"><p class="text-muted center">No search results.</p></c:when>
            <c:otherwise>
                <table class="tbl">
                    <tr><th>#</th><th>Mã đơn</th><th>Khách / Người tạo</th><th>Phim · Rạp</th><th>Phương thức</th>
                        <th>Mã giao dịch</th><th>Thời gian</th><th class="right">Số tiền</th><th>Trạng thái</th><th></th></tr>
                    <c:forEach var="p" items="${payments}">
                        <tr>
                            <td>${p.paymentId}</td>
                            <td><b>${p.bookingCode}</b><div><span class="st st-${p.bookingStatus}">${p.bookingStatus}</span></div></td>
                            <td><c:out value="${p.customerName}"/><div class="text-muted"><c:out value="${p.customerEmail}"/>
                                <c:if test="${p.customerRole != 'Customer'}"> · tại quầy</c:if></div></td>
                            <td><c:out value="${p.movieTitle}"/><div class="text-muted"><c:out value="${p.branchName}"/></div></td>
                            <td>${p.paymentMethod}</td>
                            <td style="max-width:170px;word-break:break-all" class="text-muted"><c:out value="${p.transactionCode}"/></td>
                            <td><fmt:formatDate value="${p.paymentTime}" pattern="HH:mm dd/MM/yyyy"/></td>
                            <td class="right"><b><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> ₫</b></td>
                            <td><span class="st st-${p.gatewayStatus}">${p.gatewayStatus}</span></td>
                            <td><a class="btn btn-outline btn-sm" href="${ctx}/admin/payments?action=detail&id=${p.paymentId}">Chi tiết</a></td>
                        </tr>
                    </c:forEach>
                </table>
                <div class="pager">
                    <c:forEach begin="1" end="${pages}" var="i">
                        <c:choose>
                            <c:when test="${i == page}"><span class="cur">${i}</span></c:when>
                            <c:otherwise>
                                <c:url var="pu" value="/admin/payments">
                                    <c:param name="page" value="${i}"/><c:param name="q" value="${q}"/>
                                    <c:param name="method" value="${method}"/><c:param name="status" value="${status}"/>
                                    <c:param name="branchId" value="${branchId}"/><c:param name="from" value="${from}"/>
                                    <c:param name="to" value="${to}"/>
                                </c:url>
                                <a href="${pu}">${i}</a>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</body>
</html>
