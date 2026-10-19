<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="movie" value="${info.movie}"/>
<c:set var="pageTitle" value="${movie.title}"/>
<c:set var="nav" value="movies"/>
<c:set var="movieTab" value="view"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/movies">&larr; Danh sách phim</a>
<div class="page-head">
    <h1 class="page-title"><c:out value="${movie.title}"/></h1>
    <a class="btn" href="${ctx}/manage/movies?action=edit&id=${movie.movieId}">Sửa thông tin</a>
</div>
<%@ include file="/WEB-INF/views/common/movie-tabs-hoang.jspf" %>

<div class="movie-detail">
    <div class="movie-detail-poster">
        <c:choose>
            <c:when test="${not empty info.posterUrl}"><img class="poster" src="${fn:escapeXml(info.posterUrl)}" alt="Poster ${fn:escapeXml(movie.title)}"></c:when>
            <c:otherwise><span class="poster" aria-hidden="true"></span></c:otherwise>
        </c:choose>
    </div>

    <div class="movie-detail-body">
        <p><c:set var="tagStatus" value="${movie.status}"/><%@ include file="/WEB-INF/views/common/status-tag-hoang.jspf" %></p>

        <dl class="detail-list">
            <dt>Thể loại</dt>
            <dd>
                <c:choose>
                    <c:when test="${info.genreless}"><span class="tag tag-warn">Chưa có thể loại, không được phát sóng</span></c:when>
                    <c:otherwise><c:out value="${info.genreText}"/></c:otherwise>
                </c:choose>
            </dd>
            <dt>Thời lượng</dt><dd>${movie.duration} phút</dd>
            <dt>Khởi chiếu</dt><dd>${movie.releaseDateText}</dd>
            <dt>Phân loại</dt><dd>${movie.ageRating}</dd>
            <dt>Đạo diễn</dt><dd><c:out value="${empty movie.director ? 'Chưa cập nhật' : movie.director}"/></dd>
            <dt>Ngôn ngữ</dt><dd><c:out value="${empty movie.language ? 'Chưa cập nhật' : movie.language}"/></dd>
            <dt>Chi nhánh</dt><dd><a href="${ctx}/manage/movies/branches?movieId=${movie.movieId}">${info.branchCount} chi nhánh</a></dd>
        </dl>

        <h2 class="detail-heading">Diễn viên</h2>
        <c:choose>
            <c:when test="${empty movie.castList}"><p class="meta">Chưa cập nhật diễn viên.</p></c:when>
            <c:otherwise>
                <ul class="cast-cards" aria-label="Diễn viên">
                    <c:forEach var="actor" items="${movie.castList}">
                        <li class="cast-card"><span class="cast-card-name"><c:out value="${actor}"/></span></li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>

        <h2 class="detail-heading">Nội dung</h2>
        <c:choose>
            <c:when test="${empty movie.description}"><p class="meta">Chưa có nội dung.</p></c:when>
            <c:otherwise><p class="movie-synopsis"><c:out value="${movie.description}"/></p></c:otherwise>
        </c:choose>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
