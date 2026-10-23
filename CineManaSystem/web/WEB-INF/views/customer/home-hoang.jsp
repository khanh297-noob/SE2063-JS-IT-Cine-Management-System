<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="pageTitle" value="${filtered ? filterTitle : 'Phim'}"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>
<%-- Đường dẫn trang hiện tại, để ô yêu thích quay lại đúng chỗ sau khi bấm --%>
<c:set var="query" value="${requestScope['jakarta.servlet.forward.query_string']}"/>
<c:set var="currentPath" value="/movies${empty query ? '' : '?'}${query}"/>

<c:if test="${not filtered}">
    <section class="carousel" id="promoCarousel" aria-roledescription="carousel" aria-label="Khuyến mãi">
        <div class="carousel-viewport">
            <div class="carousel-track">
                <div class="carousel-slide" role="group" aria-roledescription="slide" aria-label="1 / 3">
                    <img src="${ctx}/assets/img/banners/banner-1.webp" alt="CineGo chào mừng bạn mới, khuyến mãi 10%">
                </div>
                <div class="carousel-slide" role="group" aria-roledescription="slide" aria-label="2 / 3">
                    <img src="${ctx}/assets/img/banners/banner-2.webp" alt="Khách hàng dưới 23 tuổi: giá vé tiêu chuẩn chỉ 70.000 đồng, áp dụng cả tuần" loading="lazy">
                </div>
                <div class="carousel-slide" role="group" aria-roledescription="slide" aria-label="3 / 3">
                    <img src="${ctx}/assets/img/banners/banner-3.webp" alt="Happiness Combo bắp nước chỉ từ 149K, 27/10 đến 06/11" loading="lazy">
                </div>
            </div>
        </div>
        <button type="button" class="carousel-nav carousel-prev" aria-label="Ảnh trước">&#8249;</button>
        <button type="button" class="carousel-nav carousel-next" aria-label="Ảnh tiếp theo">&#8250;</button>
        <div class="carousel-dots" role="group" aria-label="Chọn ảnh">
            <button type="button" aria-label="Ảnh 1" aria-current="true"></button>
            <button type="button" aria-label="Ảnh 2"></button>
            <button type="button" aria-label="Ảnh 3"></button>
        </div>
    </section>
</c:if>

<c:if test="${filtered}">
    <div class="filter-head">
        <h1 class="board-title"><c:out value="${filterTitle}"/></h1>
        <a class="board-clear" href="${ctx}/movies">Xem tất cả phim</a>
    </div>
</c:if>

<div class="movie-board ${filtered ? '' : 'with-aside'}">
    <div class="board-col">
        <%-- Phim đang chiếu --%>
        <section class="board-main" aria-labelledby="showingTitle">
            <div class="board-head">
                <h2 class="board-title" id="showingTitle">Phim đang chiếu</h2>
                <span class="meta">${fn:length(movies)} phim</span>
            </div>
            <c:choose>
                <c:when test="${empty movies}">
                    <div class="empty board-empty">Không có phim đang chiếu nào phù hợp.</div>
                </c:when>
                <c:otherwise>
                    <div class="movie-grid6">
                        <c:forEach var="ms" items="${movies}">
                            <c:set var="m" value="${ms.movie}"/>
                            <c:set var="liked" value="${likedMovies.containsKey(m.movieId)}"/>
                            <c:set var="likeCount" value="${empty favoriteCounts[m.movieId] ? 0 : favoriteCounts[m.movieId]}"/>
                            <article class="mcard" id="movie-${m.movieId}">
                                <a class="mcard-poster" href="${ctx}/movies/detail?id=${m.movieId}" tabindex="-1" aria-hidden="true">
                                    <c:choose>
                                        <c:when test="${not empty ms.posterUrl}"><img src="${fn:escapeXml(ms.posterUrl)}" alt="" loading="lazy"></c:when>
                                        <c:otherwise><span class="mcard-noposter"><c:out value="${m.title}"/></span></c:otherwise>
                                    </c:choose>
                                    <span class="mcard-rating">${m.ageRating}</span>
                                </a>
                                <form class="fav-form" method="post" action="${ctx}/movies/favorite">
                                    <input type="hidden" name="movieId" value="${m.movieId}">
                                    <input type="hidden" name="back" value="${fn:escapeXml(currentPath)}#movie-${m.movieId}">
                                    <button type="submit" class="fav-btn" aria-pressed="${liked}"
                                            aria-label="${liked ? 'Bỏ yêu thích' : 'Yêu thích'} ${fn:escapeXml(m.title)}"
                                            title="${liked ? 'Bỏ yêu thích' : 'Yêu thích'}">&#9829;</button>
                                </form>
                                <a class="mcard-title" href="${ctx}/movies/detail?id=${m.movieId}"><c:out value="${m.title}"/></a>
                                <span class="mcard-likes"><span aria-hidden="true">&#9829;</span> ${likeCount} lượt yêu thích</span>
                                <%-- Trang đặt vé (/booking) do module Booking làm; khách chưa đăng nhập
                                     sẽ được AuthFilter chuyển sang /login trước --%>
                                <a class="mcard-book" href="${ctx}/booking?movieId=${m.movieId}"
                                   aria-label="Đặt vé ${fn:escapeXml(m.title)}">Đặt vé</a>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- Phim sắp chiếu: ngày khởi chiếu gần nhất trước --%>
        <section class="board-main board-soon" aria-labelledby="soonTitle">
            <div class="board-head">
                <h2 class="board-title" id="soonTitle">Phim sắp chiếu</h2>
                <span class="meta">${fn:length(comingSoon)} phim</span>
            </div>
            <c:choose>
                <c:when test="${empty comingSoon}">
                    <div class="empty board-empty">Không có phim sắp chiếu nào phù hợp.</div>
                </c:when>
                <c:otherwise>
                    <div class="movie-grid6">
                        <c:forEach var="ms" items="${comingSoon}">
                            <c:set var="m" value="${ms.movie}"/>
                            <article class="mcard">
                                <a class="mcard-poster" href="${ctx}/movies/detail?id=${m.movieId}" tabindex="-1" aria-hidden="true">
                                    <c:choose>
                                        <c:when test="${not empty ms.posterUrl}"><img src="${fn:escapeXml(ms.posterUrl)}" alt="" loading="lazy"></c:when>
                                        <c:otherwise><span class="mcard-noposter"><c:out value="${m.title}"/></span></c:otherwise>
                                    </c:choose>
                                    <span class="mcard-rating">${m.ageRating}</span>
                                    <span class="mcard-release">Khởi chiếu ${m.releaseDateText}</span>
                                </a>
                                <a class="mcard-title" href="${ctx}/movies/detail?id=${m.movieId}"><c:out value="${m.title}"/></a>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <c:if test="${not filtered}">
        <aside class="recommend" aria-labelledby="recommendTitle">
            <h2 id="recommendTitle">Đề cử tháng ${recommendMonth}</h2>
            <p class="meta">Top phim được yêu thích nhiều nhất</p>
            <c:choose>
                <c:when test="${empty recommended}"><p class="meta">Chưa có phim đang chiếu.</p></c:when>
                <c:otherwise>
                    <ol class="recommend-list">
                        <c:forEach var="ms" items="${recommended}" varStatus="st">
                            <li>
                                <span class="recommend-rank" aria-hidden="true">${st.count}</span>
                                <a href="${ctx}/movies/detail?id=${ms.movie.movieId}"><c:out value="${ms.movie.title}"/></a>
                                <c:set var="recCount" value="${empty favoriteCounts[ms.movie.movieId] ? 0 : favoriteCounts[ms.movie.movieId]}"/>
                                <span class="recommend-likes" title="${recCount} lượt yêu thích"><span aria-hidden="true">&#9829;</span> ${recCount}</span>
                            </li>
                        </c:forEach>
                    </ol>
                </c:otherwise>
            </c:choose>
        </aside>
    </c:if>
</div>

<c:if test="${not filtered}"><script src="${ctx}/assets/js/carousel-hoang.js?v=${assetVer}"></script></c:if>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
