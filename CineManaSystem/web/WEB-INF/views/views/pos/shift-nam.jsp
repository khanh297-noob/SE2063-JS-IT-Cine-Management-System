<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- 7.2 Working hours Management - phía nhân viên: vào ca / kết ca --%>
<c:set var="posTab" value="shift"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>POS CineGo - Ca Làm Việc</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<%@include file="/WEB-INF/views/common/pos-header-nam.jspf"%>

<div class="container">
    <div class="grid grid-2">
        <div class="card">
            <c:choose>
                <c:when test="${empty shift}">
                    <div class="card-title">Bắt Đầu Ca Làm Việc</div>
                    <form method="post" action="${ctx}/pos/shift">
                        <input type="hidden" name="action" value="start">
                        <div class="field">
                            <label>Rạp làm việc *</label>
                            <select name="branchId" required>
                                <c:forEach var="br" items="${branches}">
                                    <option value="${br.branchId}"><c:out value="${br.name}"/> - <c:out value="${br.city}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <c:if test="${isCashier}">
                            <div class="field">
                                <label>Tiền mặt đầu ca (tiền lẻ trong két) *</label>
                                <input type="number" name="openingFloat" min="0" step="1000" value="0" required>
                            </div>
                        </c:if>
                        <button class="btn btn-primary btn-block">Vào ca</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="card-title">Ca Đang Làm <span class="st st-Valid">ĐANG TRONG CA</span></div>
                    <div class="info-grid" style="margin-bottom:16px">
                        <div><small>Rạp</small><b><c:out value="${shift.branchName}"/></b></div>
                        <div><small>Bắt đầu</small><fmt:formatDate value="${shift.startTime}" pattern="HH:mm dd/MM/yyyy"/></div>
                        <div><small>Đã làm</small><b>${shift.hours} giờ</b></div>
                        <c:if test="${isCashier}">
                            <div><small>Tiền đầu ca</small><fmt:formatNumber value="${shift.openingFloat}" pattern="#,##0"/> ₫</div>
                        </c:if>
                    </div>
                    <c:if test="${isCashier}">
                        <div class="alert alert-info">
                            Tiền mặt phải có trong két: <b><fmt:formatNumber value="${balance.expected}" pattern="#,##0"/> ₫</b>
                            — <a href="${ctx}/pos/cash-balance">xem chi tiết</a>
                        </div>
                    </c:if>
                    <form method="post" action="${ctx}/pos/shift" onsubmit="return confirm('Kết thúc ca làm việc?')">
                        <input type="hidden" name="action" value="end">
                        <div class="field">
                            <label>Tiền mặt đếm thực tế trong két *</label>
                            <input type="number" name="countedCash" min="0" step="1000" required
                                   value="${isCashier ? balance.expected : 0}" ${isCashier ? '' : 'readonly'}>
                        </div>
                        <button class="btn btn-dark btn-block">Kết thúc ca</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="card">
            <div class="card-title">Giờ Làm Tháng Này <span class="tag">${monthHours} giờ</span></div>
            <c:choose>
                <c:when test="${empty shifts}"><p class="text-muted">Chưa có ca làm việc.</p></c:when>
                <c:otherwise>
                    <table class="tbl">
                        <tr><th>Rạp</th><th>Vào ca</th><th>Kết ca</th><th class="right">Giờ</th><c:if test="${isCashier}"><th class="right">Chênh lệch két</th></c:if></tr>
                        <c:forEach var="s" items="${shifts}">
                            <tr>
                                <td><c:out value="${s.branchName}"/></td>
                                <td><fmt:formatDate value="${s.startTime}" pattern="HH:mm dd/MM"/></td>
                                <td><c:choose><c:when test="${s.open}"><span class="st st-Pending">Đang làm</span></c:when>
                                    <c:otherwise><fmt:formatDate value="${s.endTime}" pattern="HH:mm dd/MM"/></c:otherwise></c:choose></td>
                                <td class="right">${s.hours}</td>
                                <c:if test="${isCashier}">
                                    <td class="right ${s.difference < 0 ? 'text-red' : ''}">
                                        <c:if test="${not empty s.difference}"><fmt:formatNumber value="${s.difference}" pattern="#,##0"/> ₫</c:if></td>
                                </c:if>
                            </tr>
                        </c:forEach>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
</body>
</html>
