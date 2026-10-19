<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="pageTitle" value="Poster và trailer"/>
<c:set var="nav" value="movies"/>
<c:set var="movieTab" value="media"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/movies">&larr; Danh sách phim</a>
<h1 class="page-title"><c:out value="${movie.title}"/></h1>
<%@ include file="/WEB-INF/views/common/movie-tabs-hoang.jspf" %>

<form class="form-card" method="post" action="${ctx}/manage/movies/media" style="margin-bottom:28px">
    <input type="hidden" name="action" value="add">
    <input type="hidden" name="movieId" value="${movie.movieId}">
    <div class="field-row">
        <div class="field">
            <label for="mediaType" class="required">Loại</label>
            <select id="mediaType" name="mediaType" required>
                <option value="POSTER" ${draft.mediaType == 'TRAILER' ? '' : 'selected'}>Poster</option>
                <option value="TRAILER" ${draft.mediaType == 'TRAILER' ? 'selected' : ''}>Trailer</option>
            </select>
        </div>
        <div class="field">
            <label for="caption">Chú thích</label>
            <input type="text" id="caption" name="caption" maxlength="150" value="${fn:escapeXml(draft.caption)}" placeholder="Teaser, bản chính thức...">
        </div>
    </div>
    <div class="field">
        <label for="url" class="required">Đường dẫn</label>
        <input type="url" id="url" name="url" maxlength="500" required value="${fn:escapeXml(draft.url)}"
               placeholder="https://res.cloudinary.com/... hoặc https://www.youtube.com/watch?v=...">
        <div class="hint">Poster: link ảnh (Cloudinary, CDN). Trailer: link YouTube sẽ được nhúng xem trực tiếp.</div>
    </div>
    <div class="field">
        <label><input type="checkbox" class="inline-check" name="primary" ${draft.primary ? 'checked' : ''}> Đặt làm ảnh/video chính</label>
    </div>
    <button type="submit" class="btn btn-primary">Thêm</button>
</form>

<c:set var="hasPoster" value="${false}"/>
<c:set var="hasTrailer" value="${false}"/>
<c:forEach var="m" items="${mediaList}">
    <c:if test="${m.poster}"><c:set var="hasPoster" value="${true}"/></c:if>
    <c:if test="${not m.poster}"><c:set var="hasTrailer" value="${true}"/></c:if>
</c:forEach>

<h2 class="section-title">Poster</h2>
<c:choose>
    <c:when test="${not hasPoster}"><p class="page-sub">Chưa có poster. Phim cần ít nhất một poster để bắt đầu chiếu.</p></c:when>
    <c:otherwise>
        <div class="media-grid">
            <c:forEach var="m" items="${mediaList}">
                <c:if test="${m.poster}">
                    <div class="media-item ${m.primary ? 'is-primary' : ''}">
                        <img src="${fn:escapeXml(m.url)}" alt="Poster ${fn:escapeXml(movie.title)}" loading="lazy">
                        <div class="meta">
                            <c:if test="${m.primary}"><span class="tag tag-now">Poster chính</span><br></c:if>
                            <c:out value="${m.caption}"/>
                        </div>
                        <div class="actions">
                            <c:if test="${not m.primary}">
                                <form method="post" action="${ctx}/manage/movies/media">
                                    <input type="hidden" name="action" value="primary">
                                    <input type="hidden" name="movieId" value="${movie.movieId}">
                                    <input type="hidden" name="mediaId" value="${m.mediaId}">
                                    <button class="btn btn-sm" type="submit">Đặt làm chính</button>
                                </form>
                            </c:if>
                            <form method="post" action="${ctx}/manage/movies/media">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="movieId" value="${movie.movieId}">
                                <input type="hidden" name="mediaId" value="${m.mediaId}">
                                <button class="btn btn-sm btn-danger" type="submit" data-confirm="Xoá poster này?">Xoá</button>
                            </form>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<h2 class="section-title">Trailer</h2>
<c:choose>
    <c:when test="${not hasTrailer}"><p class="page-sub">Chưa có trailer.</p></c:when>
    <c:otherwise>
        <div class="media-grid trailer-grid">
            <c:forEach var="m" items="${mediaList}">
                <c:if test="${not m.poster}">
                    <div class="media-item ${m.primary ? 'is-primary' : ''}">
                        <c:choose>
                            <c:when test="${not empty m.youtubeEmbedUrl}">
                                <div class="video"><iframe src="${m.youtubeEmbedUrl}" title="Trailer ${fn:escapeXml(movie.title)}"
                                        loading="lazy" allowfullscreen></iframe></div>
                            </c:when>
                            <c:otherwise><a href="${fn:escapeXml(m.url)}" target="_blank" rel="noopener">Mở trailer</a></c:otherwise>
                        </c:choose>
                        <div class="meta">
                            <c:if test="${m.primary}"><span class="tag tag-now">Trailer chính</span><br></c:if>
                            <c:out value="${m.caption}"/>
                        </div>
                        <div class="actions">
                            <c:if test="${not m.primary}">
                                <form method="post" action="${ctx}/manage/movies/media">
                                    <input type="hidden" name="action" value="primary">
                                    <input type="hidden" name="movieId" value="${movie.movieId}">
                                    <input type="hidden" name="mediaId" value="${m.mediaId}">
                                    <button class="btn btn-sm" type="submit">Đặt làm chính</button>
                                </form>
                            </c:if>
                            <form method="post" action="${ctx}/manage/movies/media">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="movieId" value="${movie.movieId}">
                                <input type="hidden" name="mediaId" value="${m.mediaId}">
                                <button class="btn btn-sm btn-danger" type="submit" data-confirm="Xoá trailer này?">Xoá</button>
                            </form>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
<script src="${ctx}/assets/js/confirm-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
