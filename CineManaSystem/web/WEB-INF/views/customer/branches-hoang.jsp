<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%--
  Danh sách rạp (GET /branch). BranchListServlet đặt sẵn:
    branches  các chi nhánh đang hoạt động (BranchDetailDTO: branch, hours, rooms, nowShowing, openNow)
  Bấm vào rạp nào thì mở /branch/details?id= của rạp đó.
--%>
<c:set var="pageTitle" value="Hệ thống rạp"/>
<%@ include file="/WEB-INF/views/common/branch-header-hoang.jspf" %>

<div class="cbranch">
    <section class="public-hero">
        <h1>Hệ thống rạp CineGo</h1>
        <p>${fn:length(branches)} rạp đang hoạt động. Chọn một rạp để xem giờ mở cửa, phòng chiếu và phim đang chiếu.</p>
    </section>

    <c:choose>
        <c:when test="${empty branches}">
            <div class="empty board-empty">Hiện chưa có rạp nào đang hoạt động.</div>
        </c:when>
        <c:otherwise>
            <ul class="branch-list">
                <c:forEach var="d" items="${branches}">
                    <c:set var="b" value="${d.branch}"/>
                    <li>
                        <a class="branch-card" href="${ctx}/branch/details?id=${b.branchId}">
                            <div class="branch-card-head">
                                <h2><c:out value="${b.name}"/></h2>
                                <span class="branch-open ${d.openNow ? 'open' : ''}">${d.openNow ? 'Đang mở cửa' : 'Đang đóng cửa'}</span>
                            </div>
                            <p class="branch-addr"><c:out value="${b.address}"/>, <c:out value="${b.city}"/></p>
                            <dl class="branch-facts">
                                <div><dt>Hotline</dt><dd><c:out value="${b.hotline}"/></dd></div>
                                <div>
                                    <dt>Hôm nay</dt>
                                    <dd>
                                        <c:set var="todayHours" value="${null}"/>
                                        <c:forEach var="h" items="${d.hours}">
                                            <c:if test="${h.dayOfWeek == d.todayDayOfWeek}"><c:set var="todayHours" value="${h}"/></c:if>
                                        </c:forEach>
                                        <c:choose>
                                            <c:when test="${empty todayHours}">Chưa cập nhật</c:when>
                                            <c:when test="${todayHours.closed}">Nghỉ</c:when>
                                            <c:otherwise>${todayHours.openTime} - ${todayHours.closeTime}<c:if test="${todayHours.overnight}"> (hôm sau)</c:if></c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div><dt>Phòng chiếu</dt><dd>${fn:length(d.rooms)} phòng</dd></div>
                                <div><dt>Đang chiếu</dt><dd>${fn:length(d.nowShowing)} phim</dd></div>
                            </dl>
                            <span class="branch-more">Xem chi tiết rạp <span aria-hidden="true">&rarr;</span></span>
                        </a>
                    </li>
                </c:forEach>
            </ul>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
