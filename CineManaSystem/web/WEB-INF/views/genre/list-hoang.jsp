<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="pageTitle" value="${empty selected ? 'Thể loại' : selected.name}"/>
<c:set var="nav" value="genres"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Thể loại</h1>
        <p class="page-sub">${fn:length(genres)} thể loại</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/manage/genres?action=create">Thêm thể loại</a>
</div>

<c:choose>
    <c:when test="${empty genres}">
        <div class="table-wrap">
            <div class="empty">Chưa có thể loại. <a href="${ctx}/manage/genres?action=create">Thêm thể loại đầu tiên</a></div>
        </div>
    </c:when>
    <c:otherwise>
        <%-- Container các nút thể loại: tối đa 6 cột. Ít hơn 6 nút thì chỉ có bấy nhiêu cột,
             nên chiều rộng container co theo số nút và tự giãn ra khi thêm thể loại. --%>
        <c:set var="genreCols" value="${fn:length(genres) < 6 ? fn:length(genres) : 6}"/>
        <form class="genre-card" method="get" action="${ctx}/manage/genres" aria-label="Danh sách thể loại">
            <div class="genre-buttons" style="--genre-cols:${genreCols}">
                <c:forEach var="g" items="${genres}">
                    <button type="submit" name="id" value="${g.genreId}" class="genre-btn"
                            aria-pressed="${selected.genreId == g.genreId}" title="${fn:escapeXml(g.name)}"><span class="genre-btn-text"><c:out value="${g.name}"/></span></button>
                </c:forEach>
            </div>
        </form>

        <c:choose>
            <c:when test="${empty selected}">
                <p class="meta genre-hint">Chọn một thể loại để xem chi tiết và các phim thuộc thể loại đó.</p>
            </c:when>
            <c:otherwise>
                <c:set var="used" value="${fn:length(genreMovies)}"/>
                <%-- Số phim mà thể loại này là thể loại duy nhất --%>
                <c:set var="onlyGenreCount" value="0"/>
                <c:forEach var="ms" items="${genreMovies}">
                    <c:if test="${fn:length(ms.genreNames) == 1}"><c:set var="onlyGenreCount" value="${onlyGenreCount + 1}"/></c:if>
                </c:forEach>
                <section class="genre-detail">
                    <div class="genre-detail-head">
                        <div>
                            <h2 class="section-title"><c:out value="${selected.name}"/></h2>
                            <p class="meta">
                                <c:choose>
                                    <c:when test="${empty selected.description}">Chưa có mô tả.</c:when>
                                    <c:otherwise><c:out value="${selected.description}"/></c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                        <div class="actions">
                            <a class="btn btn-sm" href="${ctx}/manage/genres?action=edit&id=${selected.genreId}">Sửa</a>
                            <form method="post" action="${ctx}/manage/genres">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="id" value="${selected.genreId}">
                                <c:set var="confirmText" value="Xoá thể loại ${selected.name}?"/>
                                <c:if test="${used > 0}">
                                    <c:set var="confirmText" value="${confirmText} Thể loại sẽ được gỡ khỏi ${used} phim."/>
                                </c:if>
                                <c:if test="${onlyGenreCount > 0}">
                                    <c:set var="confirmText" value="${confirmText} ${onlyGenreCount} phim chỉ có thể loại này sẽ không còn thể loại nào và không được phát sóng."/>
                                </c:if>
                                <button class="btn btn-sm btn-danger" type="submit"
                                        data-confirm="${fn:escapeXml(confirmText)}">Xoá</button>
                            </form>
                        </div>
                    </div>
                    <c:if test="${onlyGenreCount > 0}">
                        <p class="lock-note">${onlyGenreCount} phim chỉ có thể loại này. Nếu xoá, các phim đó sẽ không còn
                            thể loại và không được phát sóng cho tới khi thêm thể loại mới.</p>
                    </c:if>

                    <h3 class="genre-movies-title">Phim thuộc thể loại (${used})</h3>
                    <div class="table-wrap">
                        <c:choose>
                            <c:when test="${empty genreMovies}">
                                <div class="empty">Chưa có phim nào thuộc thể loại này.</div>
                            </c:when>
                            <c:otherwise>
                                <table>
                                    <thead><tr><th></th><th>Phim</th><th>Trạng thái</th><th></th></tr></thead>
                                    <tbody>
                                    <c:forEach var="ms" items="${genreMovies}">
                                        <c:set var="m" value="${ms.movie}"/>
                                        <tr>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty ms.posterUrl}"><img class="poster" src="${fn:escapeXml(ms.posterUrl)}" alt="" loading="lazy"></c:when>
                                                    <c:otherwise><span class="poster" aria-hidden="true"></span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <div class="movie-title"><a class="title-link" href="${ctx}/manage/movies?action=view&id=${m.movieId}"><c:out value="${m.title}"/></a></div>
                                                <div class="meta"><c:out value="${ms.genreText}"/></div>
                                                <div class="meta">${m.ageRating}, ${m.duration} phút, khởi chiếu ${m.releaseDateText}</div>
                                            </td>
                                            <td><c:set var="tagStatus" value="${m.status}"/><%@ include file="/WEB-INF/views/common/status-tag-hoang.jspf" %></td>
                                            <td><a class="btn btn-sm" href="${ctx}/manage/movies?action=edit&id=${m.movieId}">Sửa phim</a></td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </section>
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>
<script src="${ctx}/assets/js/confirm-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
