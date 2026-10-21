<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>
<%-- Đặt vé bước 1b: phim đã chọn + lịch chiếu của phim theo ngày, gom theo rạp.
     Servlet: BookingServlet (detail, movie, days, enabledDays, date, dateLabel, showtimesByBranch). --%>
<c:set var="pageTitle" value="Lịch chiếu ${movie.title}"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <a class="back" href="${ctx}/booking">&larr; Chọn phim khác</a>
    <ol class="bk-steps">
        <li class="active">1. Chọn phim &amp; suất chiếu</li>
        <li>2. Chọn ghế</li>
        <li>3. Đồ ăn &amp; nước</li>
        <li>4. Thanh toán VNPay</li>
        <li>5. Nhận vé QR</li>
    </ol>

    <section class="bk-panel bk-movie-head">
        <div class="cdetail-poster">
            <c:choose>
                <c:when test="${not empty detail.summary.posterUrl}"><img src="${fn:escapeXml(detail.summary.posterUrl)}" alt="Poster ${fn:escapeXml(movie.title)}"></c:when>
                <c:otherwise><span class="mcard-noposter"><c:out value="${movie.title}"/></span></c:otherwise>
            </c:choose>
        </div>
        <div>
            <h1><c:out value="${movie.title}"/> <span class="bk-age age-${movie.ageRating}">${movie.ageRating}</span></h1>
            <p class="bk-muted">${movie.duration} phút · <c:out value="${detail.summary.genreText}"/> · Khởi chiếu ${movie.releaseDateText}</p>
            <c:if test="${not empty movie.description}"><p class="movie-synopsis"><c:out value="${movie.description}"/></p></c:if>
            <a href="${ctx}/movies/detail?id=${movie.movieId}">Xem chi tiết phim &rarr;</a>
        </div>
    </section>

    <section class="bk-panel">
        <h2>Lịch chiếu</h2>
        <nav class="bk-days" aria-label="Chọn ngày">
            <c:forEach var="d" items="${days}">
                <a class="bk-day ${d.key == date ? 'current' : ''} ${enabledDays.contains(d.key) ? '' : 'disabled'}"
                   href="${ctx}/booking?movieId=${movie.movieId}&date=${d.key}" ${d.key == date ? 'aria-current="date"' : ''}>
                    <b>${d.value[0]}</b><span>${d.value[1]}</span>
                </a>
            </c:forEach>
        </nav>

        <c:choose>
            <c:when test="${empty showtimesByBranch}">
                <div class="empty">Phim chưa có suất chiếu mở bán vào ${dateLabel}. Hãy chọn ngày khác.</div>
            </c:when>
            <c:otherwise>
                <p class="bk-muted bk-small">Suất chiếu ngày ${dateLabel}. Giá đã gồm phụ thu loại phòng và cuối tuần, ghế VIP / Sweetbox cộng thêm phụ thu loại ghế.</p>
                <c:forEach var="entry" items="${showtimesByBranch}">
                    <div class="bk-branch">
                        <h3><c:out value="${entry.key}"/></h3>
                        <div class="bk-times">
                            <c:forEach var="s" items="${entry.value}">
                                <a class="bk-time" href="${ctx}/booking/seats?showtimeId=${s.showtimeId}"
                                   title="${fn:escapeXml(s.roomName)} · còn ${s.availableCount} ghế">
                                    <b>${s.startStr}</b>
                                    <span><c:out value="${s.roomType}"/> · từ <fmt:formatNumber value="${s.basePrice}" pattern="#,##0"/> ₫</span>
                                    <span>Còn ${s.availableCount} ghế</span>
                                </a>
                            </c:forEach>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
