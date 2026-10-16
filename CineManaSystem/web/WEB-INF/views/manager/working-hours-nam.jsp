<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 7.2 Working hours Management - phía quản lý --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Giờ Làm Nhân Viên</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo <span class="crumb">Quản Lý Giờ Làm (Working Hours)</span></div>
    <div class="user"><b><c:out value="${me.fullName}"/></b><br><c:out value="${me.role}"/></div>
</header>
<div class="container">
    <div class="card">
        <form method="get" class="row" style="flex-wrap:wrap">
            <div><label>Nhân viên</label>
                <select name="staffId">
                    <option value="0">Tất cả</option>
                    <c:forEach var="s" items="${staffList}">
                        <option value="${s.userId}" ${staffId == s.userId ? 'selected' : ''}><c:out value="${s.fullName}"/> (${s.role})</option>
                    </c:forEach>
                </select></div>
            <div><label>Rạp</label>
                <select name="branchId">
                    <option value="0">Tất cả</option>
                    <c:forEach var="br" items="${branches}">
                        <option value="${br.branchId}" ${branchId == br.branchId ? 'selected' : ''}><c:out value="${br.name}"/></option>
                    </c:forEach>
                </select></div>
            <div><label>Từ ngày</label><input type="date" name="from" value="${from}"></div>
            <div><label>Đến ngày</label><input type="date" name="to" value="${to}"></div>
            <div class="auto"><button class="btn btn-primary">Lọc</button></div>
        </form>
        <c:if test="${not empty dateError}"><div class="field-error">${dateError}</div></c:if>
    </div>

    <div class="grid grid-2">
        <div class="card">
            <div class="card-title">Chi Tiết Ca Làm <span class="tag">${fn:length(shifts)} ca</span></div>
            <c:choose>
                <c:when test="${empty shifts}"><p class="text-muted center">No data available for the selected period.</p></c:when>
                <c:otherwise>
                    <table class="tbl">
                        <tr><th>Nhân viên</th><th>Rạp</th><th>Vào ca</th><th>Kết ca</th><th class="right">Giờ</th><th class="right">Chênh lệch két</th></tr>
                        <c:forEach var="s" items="${shifts}">
                            <tr>
                                <td><c:out value="${s.staffName}"/><div class="text-muted">${s.role}</div></td>
                                <td><c:out value="${s.branchName}"/></td>
                                <td><fmt:formatDate value="${s.startTime}" pattern="HH:mm dd/MM/yyyy"/></td>
                                <td><c:choose><c:when test="${s.open}"><span class="st st-Pending">Đang làm</span></c:when>
                                    <c:otherwise><fmt:formatDate value="${s.endTime}" pattern="HH:mm dd/MM/yyyy"/></c:otherwise></c:choose></td>
                                <td class="right">${s.hours}</td>
                                <td class="right ${s.difference < 0 ? 'text-red' : ''}">
                                    <c:if test="${not empty s.difference}"><fmt:formatNumber value="${s.difference}" pattern="#,##0"/> ₫</c:if></td>
                            </tr>
                        </c:forEach>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="card">
            <div class="card-title">Tổng Giờ Theo Nhân Viên</div>
            <table class="tbl">
                <tr><th>Nhân viên</th><th class="right">Tổng giờ</th></tr>
                <c:forEach var="e" items="${totals}">
                    <tr><td><c:out value="${e.key}"/></td><td class="right"><b>${e.value}</b></td></tr>
                </c:forEach>
            </table>
        </div>
    </div>
</div>
</body>
</html>
