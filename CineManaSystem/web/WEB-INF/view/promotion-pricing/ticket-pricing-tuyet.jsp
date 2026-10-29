<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Co dinh ngon ngu Viet Nam de so tien luon co dang 250.000 du trinh duyet dung ngon ngu nao --%>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageUrl" value="${pageContext.request.contextPath}/promotion-price-manage/ticket-pricing"/>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý giá vé - CineGo Management</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/promotion-pricing.css">
    </head>
    <body>
        <%-- Khung chung của phân hệ: menu bên trái chọn theo khanhFeature/khanhActive --%>
        <c:set var="khanhFeature" value="promotion" scope="request"/>
        <c:set var="khanhActive" value="ticket-prices" scope="request"/>
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
                        <h1 class="page-title"><i class="fa-solid fa-ticket"></i> Quản lý giá vé</h1>
                        <p class="page-subtitle">Giá gốc là giá ban đầu của suất chiếu và cố định; giá hiện tại là giá đang bán và có thể thay đổi. Vé đã bán giữ nguyên giá cũ, giá mới chỉ áp dụng cho vé bán sau đó.</p>
                    </div>
                </div>

                <c:if test="${not empty flashMessage}"><div class="alert alert-success"><i class="fa-solid fa-circle-check"></i> <c:out value="${flashMessage}"/></div></c:if>
                <c:if test="${not empty flashError}"><div class="alert alert-error"><i class="fa-solid fa-circle-exclamation"></i> <c:out value="${flashError}"/></div></c:if>
                <c:if test="${not empty filterError}"><div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> <c:out value="${filterError}"/></div></c:if>

                <%-- Bộ lọc suất chiếu (chi nhánh chọn ở ô Rạp quản lý phía trên) --%>
                <div class="card">
                    <form method="get" action="${pageUrl}" class="filter-row">
                        <c:if test="${not empty selectedBranchId}"><input type="hidden" name="branchId" value="${selectedBranchId}"></c:if>
                            <div class="filter-group">
                                <label class="filter-label" for="movieId">Phim</label>
                                <select id="movieId" name="movieId" class="filter-select">
                                    <option value="">Tất cả phim</option>
                                <c:forEach var="m" items="${movies}">
                                    <option value="${m.movieID}" ${selectedMovieId == m.movieID ? 'selected' : ''}><c:out value="${m.title}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="filter-group">
                            <label class="filter-label" for="fromDate">Từ ngày chiếu</label>
                            <input id="fromDate" type="date" name="fromDate" class="filter-input" value="${fromDate}">
                        </div>
                        <div class="filter-group">
                            <label class="filter-label" for="toDate">Đến ngày chiếu</label>
                            <input id="toDate" type="date" name="toDate" class="filter-input" value="${toDate}">
                        </div>
                        <div class="filter-actions">
                            <button type="submit" class="btn-filter"><i class="fa-solid fa-filter"></i> Lọc</button>
                            <a href="${pageUrl}${empty selectedBranchId ? '' : '?branchId='}${selectedBranchId}" class="btn-reset" title="Đặt lại bộ lọc"><i class="fa-solid fa-rotate-left"></i></a>
                        </div>
                    </form>
                </div>

                <%-- Đổi giá hàng loạt cho các suất đang lọc --%>
                <c:if test="${editableCount > 0}">
                    <div class="card">
                        <form method="post" action="${pageUrl}" class="filter-row"
                              onsubmit="return confirm('Đổi giá hiện tại của ${editableCount} suất chiếu sắp tới đang hiển thị thành giá mới?');">
                            <input type="hidden" name="action" value="bulkPrice">
                            <%@ include file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp" %>
                            <div class="filter-group">
                                <label class="filter-label" for="bulkPrice">Đổi giá hiện tại hàng loạt cho ${editableCount} suất sắp tới đang hiển thị (đồng)</label>
                                <input id="bulkPrice" type="text" inputmode="numeric" name="price" class="filter-input" placeholder="Ví dụ 85000" autocomplete="off">
                            </div>
                            <div class="filter-actions">
                                <button type="submit" class="btn-filter"><i class="fa-solid fa-layer-group"></i> Áp dụng hàng loạt</button>
                            </div>
                        </form>
                    </div>
                </c:if>

                <%-- Danh sách suất chiếu --%>
                <div class="card">
                    <div class="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Chi nhánh</th><th>Phim</th><th>Phòng</th><th>Ngày</th><th>Giờ</th>
                                    <th>Trạng thái</th><th class="align-right">Vé đã bán</th><th class="align-right">Giá gốc (cố định)</th><th>Giá hiện tại (đồng)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty rows}">
                                        <c:forEach var="r" items="${rows}">
                                            <tr>
                                                <td><c:out value="${r.branchName}"/></td>
                                                <td class="cell-strong"><c:out value="${r.movieTitle}"/></td>
                                                <td><c:out value="${r.roomName}"/> <span class="tag"><c:out value="${r.roomType}"/></span></td>
                                                <td><fmt:formatDate value="${r.showDate}" pattern="dd/MM/yyyy"/></td>
                                                <td><fmt:formatDate value="${r.startTime}" pattern="HH:mm"/></td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${r.editable && r.status == 'Open'}"><span class="badge badge-green">Đang bán</span></c:when>
                                                        <c:when test="${r.editable}"><span class="badge badge-gray">Chưa mở bán</span></c:when>
                                                        <c:when test="${r.status == 'Closed'}"><span class="badge badge-gray">Đã đóng</span></c:when>
                                                        <c:otherwise><span class="badge badge-gray">Đã chiếu</span></c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="align-right">${r.soldTickets}</td>
                                                <td class="align-right"><fmt:formatNumber value="${r.originalPrice}" pattern="#,##0"/> ₫</td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${r.editable}">
                                                            <form method="post" action="${pageUrl}" class="inline-form">
                                                                <input type="hidden" name="action" value="updatePrice">
                                                                <input type="hidden" name="showtimeId" value="${r.showtimeId}">
                                                                <%@ include file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp" %>
                                                                <input class="price-input" type="text" inputmode="numeric" name="price" autocomplete="off"
                                                                       value="<fmt:formatNumber value='${r.currentPrice}' maxFractionDigits='0' groupingUsed='false'/>"
                                                                       aria-label="Giá hiện tại suất chiếu ${r.showtimeId}">
                                                                <button type="submit" class="btn-save">Lưu</button>
                                                            </form>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span title="Suất chiếu đã bắt đầu hoặc đã kết thúc, không thể đổi giá">
                                                                <fmt:formatNumber value="${r.currentPrice}" pattern="#,##0"/> ₫
                                                            </span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr><td colspan="9" class="empty-cell"><i class="fa-solid fa-folder-open empty-icon"></i>Không có suất chiếu nào trong khoảng đã chọn.</td></tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${truncated}"><p class="note">Chỉ hiển thị ${maxRows} suất đầu tiên. Hãy thu hẹp bộ lọc để xem các suất còn lại.</p></c:if>
                        <p class="note">Chỉ đổi được giá của suất chưa bắt đầu chiếu. Giá vé cuối = giá hiện tại + phụ thu loại ghế (phụ thu loại phòng đã nằm trong giá gốc khi tạo suất). Giá gốc được tính lại từ phụ thu loại phòng và ngày chiếu nên không sửa được.</p>
                    </div>

                <%-- Phụ thu: áp dụng cho mọi chi nhánh nên chỉ Admin sửa --%>
                <div class="two-col">
                    <div class="card">
                        <h2 class="card-title">Phụ thu loại phòng</h2>
                        <c:if test="${not isAdminUser}"><p class="note">Phụ thu chung do Admin đặt. Bạn có thể cộng hoặc trừ (nhập số âm) vào giá hiện tại các suất sắp chiếu của loại phòng đó tại chi nhánh mình; chi nhánh khác không bị ảnh hưởng.</p></c:if>
                            <table>
                                <thead><tr><th>Loại phòng</th><th>Phụ thu chung (đồng)</th><c:if test="${not isAdminUser}"><th>Điều chỉnh cho chi nhánh mình (đồng)</th></c:if></tr></thead>
                                <tbody>
                                <c:forEach var="t" items="${roomTypes}">
                                    <tr>
                                        <td class="cell-strong"><c:out value="${t.name}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${isAdminUser}">
                                                    <form method="post" action="${pageUrl}" class="inline-form">
                                                        <input type="hidden" name="action" value="roomTypeSurcharge">
                                                        <input type="hidden" name="id" value="${t.id}">
                                                        <%@ include file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp" %>
                                                        <input class="price-input" type="text" inputmode="numeric" name="price" autocomplete="off"
                                                               value="<fmt:formatNumber value='${t.surcharge}' maxFractionDigits='0' groupingUsed='false'/>"
                                                               aria-label="Phụ thu loại phòng ${t.name}">
                                                        <button type="submit" class="btn-save">Lưu</button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise><fmt:formatNumber value="${t.surcharge}" pattern="#,##0"/> ₫</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <c:if test="${not isAdminUser}">
                                            <td>
                                                <form method="post" action="${pageUrl}" class="inline-form"
                                                      onsubmit="return confirm('Cộng/trừ khoản này vào giá hiện tại các suất sắp chiếu của loại phòng này tại chi nhánh bạn?');">
                                                    <input type="hidden" name="action" value="roomTypeAdjust">
                                                    <input type="hidden" name="id" value="${t.id}">
                                                    <%@ include file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp" %>
                                                    <input class="price-input" type="text" name="delta" autocomplete="off"
                                                           placeholder="+10000 hoặc -5000" aria-label="Điều chỉnh giá loại phòng ${t.name}">
                                                    <button type="submit" class="btn-save">Áp dụng</button>
                                                </form>
                                            </td>
                                        </c:if>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <div class="card">
                        <h2 class="card-title">Phụ thu loại ghế</h2>
                        <c:if test="${not isAdminUser}"><p class="note">Phụ thu áp dụng cho mọi chi nhánh nên chỉ Admin được sửa.</p></c:if>
                            <table>
                                <thead><tr><th>Loại ghế</th><th>Phụ thu (đồng)</th></tr></thead>
                                <tbody>
                                <c:forEach var="t" items="${seatTypes}">
                                    <tr>
                                        <td class="cell-strong"><c:out value="${t.name}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${isAdminUser}">
                                                    <form method="post" action="${pageUrl}" class="inline-form">
                                                        <input type="hidden" name="action" value="seatTypeSurcharge">
                                                        <input type="hidden" name="id" value="${t.id}">
                                                        <%@ include file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp" %>
                                                        <input class="price-input" type="text" inputmode="numeric" name="price" autocomplete="off"
                                                               value="<fmt:formatNumber value='${t.surcharge}' maxFractionDigits='0' groupingUsed='false'/>"
                                                               aria-label="Phụ thu loại ghế ${t.name}">
                                                        <button type="submit" class="btn-save">Lưu</button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise><fmt:formatNumber value="${t.surcharge}" pattern="#,##0"/> ₫</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>

            </main>
        </div>
    </body>
</html>
