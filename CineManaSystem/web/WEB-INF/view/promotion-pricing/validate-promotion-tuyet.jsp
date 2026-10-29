<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Co dinh ngon ngu Viet Nam de so tien luon co dang 250.000 du trinh duyet dung ngon ngu nao --%>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Kiểm tra mã khuyến mãi - CineGo Management</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/promotion-pricing.css">
    </head>
    <body>
        <%-- Khung chung của phân hệ: menu bên trái chọn theo khanhFeature/khanhActive --%>
        <c:set var="khanhFeature" value="promotion" scope="request"/>
        <c:set var="khanhActive" value="validate" scope="request"/>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="pp-page">

                <%-- Chọn rạp quản lý: trước đây nằm trên thanh điều hướng, nay ở đầu phần nội dung --%>
                <c:if test="${not empty scopeWarning}">
                    <div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> <c:out value="${scopeWarning}"/></div>
                </c:if>
                <div class="branch-bar">

                    <div>
                        <c:choose>
                            <c:when test="${branches.size() > 1}">
                                <form method="get" action="${pageContext.request.contextPath}${shellAction}" class="branch-bar-form">
                                    <c:if test="${not empty param.fromDate}"><input type="hidden" name="fromDate" value="<c:out value='${param.fromDate}'/>"></c:if>
                                    <c:if test="${not empty param.toDate}"><input type="hidden" name="toDate" value="<c:out value='${param.toDate}'/>"></c:if>
                                    <c:if test="${not empty param.type}"><input type="hidden" name="type" value="<c:out value='${param.type}'/>"></c:if>
                                    <c:if test="${not empty param.movieId}"><input type="hidden" name="movieId" value="<c:out value='${param.movieId}'/>"></c:if>
                                        <div class="branch-pill">
                                            <i class="fa-solid fa-location-dot"></i> Rạp:
                                            <select name="branchId" onchange="this.form.submit()">
                                                <option value="">Tất cả chi nhánh</option>
                                            <c:forEach var="b" items="${branches}">
                                                <option value="${b.branchID}" ${selectedBranchId == b.branchID ? 'selected' : ''}>
                                                    <c:out value="${b.name}"/> (#${b.branchID < 10 ? '0' : ''}${b.branchID})
                                                </option>
                                            </c:forEach>
                                            </select>
                                        </div>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <div class="branch-pill">
                                    <i class="fa-solid fa-location-dot"></i> Rạp: <b><c:out value="${branchPill}"/></b>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>


                <div class="page-header">
                    <div>
                        <h1 class="page-title"><i class="fa-solid fa-circle-check"></i> Kiểm tra mã khuyến mãi</h1>
                        <p class="page-subtitle">Nhập mã và giá trị đơn hàng để biết mã có áp dụng được không và số tiền được giảm.</p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${noBranch and not isAdminUser}">
                        <%-- Chỉ hiện lỗi khi nhân viên thường không có chi nhánh; Admin luôn có thể chọn --%>
                        <div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> Tài khoản của bạn chưa được gán chi nhánh nên chưa kiểm tra được mã. Hãy liên hệ Admin.</div>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty formError}"><div class="alert alert-error"><i class="fa-solid fa-circle-exclamation"></i> <c:out value="${formError}"/></div></c:if>

                            <div class="card narrow">
                                <form method="post" action="${pageContext.request.contextPath}/promotion-price-manage/validate-promotion" class="stack-form">
                                <%-- Giữ lại branchId đang chọn khi POST để admin không bị reset về chi nhánh mặc định --%>
                                <c:if test="${not empty selectedBranchId}">
                                    <input type="hidden" name="branchId" value="${selectedBranchId}">
                                </c:if>
                                <div class="filter-group">
                                    <span class="filter-label">Chi nhánh</span>
                                    <c:choose>
                                        <c:when test="${isAdminUser and branches.size() > 1}">
                                            <%-- Admin: cho chọn chi nhánh để kiểm tra mã theo từng rạp --%>
                                            <select name="branchId" class="filter-input" onchange="this.form.submit()">
                                                <option value="">-- Chọn chi nhánh --</option>
                                                <c:forEach var="b" items="${branches}">
                                                    <option value="${b.branchID}" ${selectedBranchId == b.branchID ? 'selected' : ''}>
                                                        <c:out value="${b.name}"/>
                                                    </option>
                                                </c:forEach>
                                            </select>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="static-value"><i class="fa-solid fa-location-dot"></i> <c:out value="${branchName}"/></div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="filter-group">
                                    <label class="filter-label" for="code">Mã khuyến mãi</label>
                                    <input id="code" type="text" name="code" class="filter-input" maxlength="30" autocomplete="off" autofocus
                                           value="<c:out value='${code}'/>" placeholder="Ví dụ SALE50">
                                </div>
                                <div class="filter-group">
                                    <label class="filter-label" for="orderAmount">Giá trị đơn hàng trước giảm giá (đồng)</label>
                                    <input id="orderAmount" type="text" inputmode="numeric" name="orderAmount" class="filter-input" autocomplete="off"
                                           value="<c:out value='${orderAmount}'/>" placeholder="Ví dụ 250000">
                                </div>
                                <div class="filter-actions">
                                    <button type="submit" class="btn-filter"><i class="fa-solid fa-magnifying-glass"></i> Kiểm tra</button>
                                </div>
                            </form>
                        </div>

                        <c:if test="${not empty result}">
                            <div class="card narrow result-box ${result.valid ? 'result-ok' : 'result-bad'}">
                                <div class="result-head">
                                    <i class="fa-solid ${result.valid ? 'fa-circle-check' : 'fa-circle-xmark'}"></i>
                                    <div>
                                        <h2><c:out value="${result.code}"/> — ${result.valid ? 'Áp dụng được' : 'Không áp dụng được'}</h2>
                                        <p><c:out value="${result.message}"/></p>
                                    </div>
                                </div>
                                <c:if test="${result.valid}">
                                    <dl class="result-lines">
                                        <div><dt>Giá trị đơn hàng</dt><dd><fmt:formatNumber value="${result.orderAmount}" pattern="#,##0"/> ₫</dd></div>
                                        <div><dt>Được giảm</dt><dd class="minus">- <fmt:formatNumber value="${result.discountAmount}" pattern="#,##0"/> ₫</dd></div>
                                        <div class="total"><dt>Khách phải trả</dt><dd><fmt:formatNumber value="${result.finalAmount}" pattern="#,##0"/> ₫</dd></div>
                                        <div><dt>Lượt dùng còn lại</dt>
                                            <dd><c:choose>
                                                    <c:when test="${result.usesLeft == null}">Không giới hạn</c:when>
                                                    <c:otherwise>${result.usesLeft} (trước khi tính đơn này)</c:otherwise>
                                                </c:choose></dd></div>
                                    </dl>
                                    <p class="note">Đây chỉ là kiểm tra. Mã chỉ được tính là đã dùng khi đơn hàng được tạo kèm mã này.</p>
                                </c:if>
                            </div>
                        </c:if>
                    </c:otherwise>
                </c:choose>

            </main>
        </div>
    </body>
</html>
