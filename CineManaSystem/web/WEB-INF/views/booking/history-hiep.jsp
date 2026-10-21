<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>
<%-- Vé của tôi (lịch sử đặt vé online). Servlet: MyBookingServlet (filter, bookings, totalCount). --%>
<c:set var="pageTitle" value="Vé của tôi"/>
<c:set var="extraCss" value="booking-hiep"/>
<c:set var="customerNav" value="tickets"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <div class="page-head">
        <div>
            <h1 class="page-title">Vé của tôi</h1>
            <p class="page-sub">${totalCount} đơn đặt vé online</p>
        </div>
        <div class="actions">
            <a class="btn" href="${ctx}/customer/profile">Hồ sơ cá nhân</a>
            <a class="btn" href="${ctx}/payment/history">Lịch sử thanh toán</a>
            <a class="btn btn-primary" href="${ctx}/booking">Đặt vé mới</a>
        </div>
    </div>

    <nav class="bk-filter" aria-label="Lọc đơn">
        <a class="${filter == 'all' ? 'current' : ''}" href="?filter=all">Tất cả</a>
        <a class="${filter == 'upcoming' ? 'current' : ''}" href="?filter=upcoming">Sắp chiếu</a>
        <a class="${filter == 'past' ? 'current' : ''}" href="?filter=past">Đã chiếu</a>
        <a class="${filter == 'cancelled' ? 'current' : ''}" href="?filter=cancelled">Đã hủy</a>
    </nav>

    <c:choose>
        <c:when test="${empty bookings}">
            <div class="empty">Không có đơn nào. <a href="${ctx}/booking">Đặt vé ngay &rarr;</a></div>
        </c:when>
        <c:otherwise>
            <div class="bk-history">
                <c:forEach var="b" items="${bookings}">
                    <a class="bk-history-item" href="${ctx}/booking/detail?id=${b.bookingId}">
                        <div class="bk-h-date">
                            <b><fmt:formatDate value="${b.startTime}" pattern="HH:mm"/></b>
                            <span><fmt:formatDate value="${b.showDate}" pattern="dd/MM/yyyy"/></span>
                        </div>
                        <div class="bk-h-main">
                            <b><c:out value="${b.movieTitle}"/></b>
                            <span class="bk-muted bk-small"><c:out value="${b.branchName}"/> · <c:out value="${b.roomName}"/> · Ghế ${b.seatLabels}</span>
                            <span class="bk-muted bk-small bk-mono">${b.bookingCode} · đặt lúc <fmt:formatDate value="${b.bookingDate}" pattern="HH:mm dd/MM/yyyy"/></span>
                        </div>
                        <div class="bk-h-side">
                            <c:choose>
                                <c:when test="${b.status == 'Pending'}"><span class="bk-badge Pending">Chờ thanh toán</span></c:when>
                                <c:when test="${b.status == 'Cancelled'}"><span class="bk-badge Cancelled">Đã hủy</span></c:when>
                                <c:when test="${b.allCheckedIn}"><span class="bk-badge">Đã check-in</span></c:when>
                                <c:when test="${b.showFinished}"><span class="bk-badge">Đã chiếu</span></c:when>
                                <c:otherwise><span class="bk-badge Confirmed">Đã thanh toán</span></c:otherwise>
                            </c:choose>
                            <b><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> ₫</b>
                            <c:if test="${b.status == 'Confirmed' and not b.showFinished}"><span class="bk-small" style="color: var(--velvet); font-weight: 600">Xem vé QR &rarr;</span></c:if>
                        </div>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>

    <p class="bk-muted bk-small" style="margin-top: 24px">Muốn ngừng sử dụng tài khoản? <a href="${ctx}/customer/deactivate">Vô hiệu hóa tài khoản</a></p>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
