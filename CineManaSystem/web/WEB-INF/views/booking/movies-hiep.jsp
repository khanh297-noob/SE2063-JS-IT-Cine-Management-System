<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="pageTitle" value="Đặt vé"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <ol class="bk-steps">
        <li class="active">1. Chọn phim &amp; suất chiếu</li>
        <li>2. Chọn ghế</li>
        <li>3. Đồ ăn &amp; nước</li>
        <li>4. Thanh toán VNPay</li>
        <li>5. Nhận vé QR</li>
    </ol>

    <section class="board-main" aria-labelledby="pickTitle">
        <div class="board-head">
            <h1 class="board-title" id="pickTitle">Chọn phim bạn muốn xem</h1>
            <span class="meta">${fn:length(movies)} phim đang chiếu</span>
        </div>
        <c:choose>
            <c:when test="${empty movies}">
                <div class="empty board-empty">Hiện chưa có phim đang chiếu.</div>
            </c:when>
            <c:otherwise>
                <div class="movie-grid6">
                    <c:forEach var="ms" items="${movies}">
                        <c:set var="m" value="${ms.movie}"/>
                        <article class="mcard">
                            <a class="mcard-poster" href="${ctx}/booking?movieId=${m.movieId}" tabindex="-1" aria-hidden="true">
                                <c:choose>
                                    <c:when test="${not empty ms.posterUrl}"><img src="${fn:escapeXml(ms.posterUrl)}" alt="" loading="lazy"></c:when>
                                    <c:otherwise><span class="mcard-noposter"><c:out value="${m.title}"/></span></c:otherwise>
                                </c:choose>
                                <span class="mcard-rating">${m.ageRating}</span>
                            </a>
                            <a class="mcard-title" href="${ctx}/booking?movieId=${m.movieId}"><c:out value="${m.title}"/></a>
                            <span class="meta">${m.duration} phút · <c:out value="${ms.genreText}"/></span>
                            <a class="mcard-book" href="${ctx}/booking?movieId=${m.movieId}">Xem lịch chiếu</a>
                        </article>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
