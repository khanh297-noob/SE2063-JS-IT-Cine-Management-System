<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="movie" value="${info.movie}"/>
<c:set var="pageTitle" value="Trạng thái phim"/>
<c:set var="nav" value="movies"/>
<c:set var="movieTab" value="status"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/movies">&larr; Danh sách phim</a>
<h1 class="page-title"><c:out value="${movie.title}"/></h1>
<%@ include file="/WEB-INF/views/common/movie-tabs-hoang.jspf" %>

<div class="status-flow" aria-label="Vòng đời phim">
    <span class="status-step ${movie.status == 'Coming Soon' ? 'current' : ''}">Sắp chiếu</span>
    <span aria-hidden="true">&rsaquo;</span>
    <span class="status-step ${movie.status == 'Now Showing' ? 'current' : ''}">Đang chiếu</span>
    <span aria-hidden="true">&rsaquo;</span>
    <span class="status-step ${movie.status == 'End of Screening' ? 'current' : ''}">Ngưng chiếu</span>
</div>

<c:forEach var="target" items="${info.allowedTargets}">
    <section class="transition-card">
        <c:choose>
            <c:when test="${target == 'Now Showing'}">
                <h2>${movie.status == 'End of Screening' ? 'Chiếu lại' : 'Bắt đầu chiếu'}</h2>
                <p class="meta">Khách thấy phim ở các chi nhánh đã chọn và có thể đặt vé.</p>
                <ul class="checklist">
                    <li class="${info.released ? 'ok' : 'fail'}">Đã tới ngày khởi chiếu (${movie.releaseDateText})</li>
                    <li class="${info.genreCount > 0 ? 'ok' : 'fail'}">
                        Có thể loại: ${info.genreCount}
                        <c:if test="${info.genreCount == 0}">(<a href="${ctx}/manage/movies?action=edit&id=${movie.movieId}">thêm thể loại</a>)</c:if>
                    </li>
                    <li class="${info.branchCount > 0 ? 'ok' : 'fail'}">
                        Có chi nhánh chiếu: ${info.branchCount}
                        <c:if test="${info.branchCount == 0}">(<a href="${ctx}/manage/movies/branches?movieId=${movie.movieId}">chọn chi nhánh</a>)</c:if>
                    </li>
                    <li class="${info.posterCount > 0 ? 'ok' : 'fail'}">
                        Có poster: ${info.posterCount}
                        <c:if test="${info.posterCount == 0}">(<a href="${ctx}/manage/movies/media?movieId=${movie.movieId}">thêm poster</a>)</c:if>
                    </li>
                </ul>
                <c:set var="ready" value="${info.readyToShow}"/>
                <c:set var="buttonText" value="${movie.status == 'End of Screening' ? 'Chiếu lại' : 'Bắt đầu chiếu'}"/>
            </c:when>
            <c:otherwise>
                <h2>${movie.status == 'Coming Soon' ? 'Huỷ chiếu' : 'Ngưng chiếu'}</h2>
                <p class="meta">Phim không còn hiển thị cho khách. Cấu hình chi nhánh bị khoá cho tới khi chiếu lại.</p>
                <ul class="checklist">
                    <li class="${info.readyToEnd ? 'ok' : 'fail'}">Không còn suất chiếu sắp tới (hiện có ${info.upcomingShowtimes})</li>
                </ul>
                <c:set var="ready" value="${info.readyToEnd}"/>
                <c:set var="buttonText" value="${movie.status == 'Coming Soon' ? 'Huỷ chiếu' : 'Ngưng chiếu'}"/>
            </c:otherwise>
        </c:choose>
        <form method="post" action="${ctx}/manage/movies/status">
            <input type="hidden" name="movieId" value="${movie.movieId}">
            <input type="hidden" name="status" value="${target}">
            <button type="submit" class="btn ${target == 'Now Showing' ? 'btn-primary' : 'btn-danger'}" ${ready ? '' : 'disabled'}>${buttonText}</button>
            <c:if test="${not ready}"><span class="meta">Hoàn tất các mục đỏ để mở nút này.</span></c:if>
        </form>
    </section>
</c:forEach>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
