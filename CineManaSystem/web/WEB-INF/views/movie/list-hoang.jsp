<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="pageTitle" value="Phim"/>
<c:set var="nav" value="movies"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Phim</h1>
        <p class="page-sub">${fn:length(movies)} phim</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/manage/movie?action=create">Thêm phim</a>
</div>

<%-- BR-MS-03: phim "Coming Soon" đã quá ngày khởi chiếu nhưng chưa đủ điều kiện chiếu (MovieAdminServlet đặt sẵn).
     Khách không thấy các phim này ở mục "Phim sắp chiếu". --%>
<c:if test="${not empty overdueMovies}">
    <details class="notice notice-error">
        <summary>${fn:length(overdueMovies)} phim "Sắp chiếu" đã quá ngày khởi chiếu nhưng chưa đủ điều kiện chiếu.
            Khách không còn thấy các phim này ở mục "Phim sắp chiếu". Bổ sung chi nhánh / thể loại / poster để phim
            tự chuyển sang Đang chiếu, hoặc sửa ngày khởi chiếu, hoặc chuyển sang Ngưng chiếu.</summary>
        <ul><c:forEach var="o" items="${overdueMovies}"><li><c:out value="${o}"/></li></c:forEach></ul>
    </details>
</c:if>

<form class="filters" method="get" action="${ctx}/manage/movie">
    <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Tìm theo tên phim" aria-label="Tìm phim">
    <select name="status" aria-label="Trạng thái">
        <option value="">Mọi trạng thái</option>
        <option value="Now Showing" ${status == 'Now Showing' ? 'selected' : ''}>Đang chiếu</option>
        <option value="Coming Soon" ${status == 'Coming Soon' ? 'selected' : ''}>Sắp chiếu</option>
        <option value="End of Screening" ${status == 'End of Screening' ? 'selected' : ''}>Ngưng chiếu</option>
    </select>
    <select name="genreId" aria-label="Thể loại">
        <option value="">Mọi thể loại</option>
        <c:forEach var="g" items="${genres}">
            <option value="${g.genreId}" ${g.genreId == genreId ? 'selected' : ''}><c:out value="${g.name}"/></option>
        </c:forEach>
    </select>
    <button class="btn" type="submit">Lọc</button>
</form>

<div class="table-wrap">
    <c:choose>
        <c:when test="${empty movies}">
            <div class="empty">Không có phim nào khớp bộ lọc.
                <c:if test="${not empty q or not empty status or not empty genreId}"><a href="${ctx}/manage/movie">Xoá bộ lọc</a></c:if></div>
        </c:when>
        <c:otherwise>
            <table>
                <thead><tr><th></th><th>Phim</th><th>Trạng thái</th><th class="hide-sm">Chi nhánh</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="ms" items="${movies}">
                    <c:set var="m" value="${ms.movie}"/>
                    <tr>
                        <td>
                            <c:choose>
                                <c:when test="${not empty ms.posterUrl}"><img class="poster" src="${fn:escapeXml(ms.posterUrl)}" alt="" loading="lazy"></c:when>
                                <c:otherwise><span class="poster" aria-hidden="true"></span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <div class="movie-title"><a class="title-link" href="${ctx}/manage/movie?action=view&id=${m.movieId}"><c:out value="${m.title}"/></a></div>
                            <div class="meta"><c:choose><c:when test="${ms.genreless}"><span class="tag tag-warn">Chưa có thể loại, không được phát sóng</span></c:when><c:otherwise><c:out value="${ms.genreText}"/></c:otherwise></c:choose></div>
                            <div class="meta">${m.ageRating}, ${m.duration} phút, khởi chiếu ${m.releaseDateText}</div>
                        </td>
                        <td><c:set var="tagStatus" value="${m.status}"/><%@ include file="/WEB-INF/views/common/status-tag-hoang.jspf" %></td>
                        <td class="hide-sm">
                            <a href="${ctx}/manage/movie/branches?movieId=${m.movieId}">${ms.branchCount} chi nhánh</a>
                        </td>
                        <td>
                            <div class="actions">
                                <a class="btn btn-sm" href="${ctx}/manage/movie?action=edit&id=${m.movieId}">Sửa</a>
                                <a class="btn btn-sm" href="${ctx}/manage/movie/media?movieId=${m.movieId}">Poster</a>
                                <a class="btn btn-sm" href="${ctx}/manage/movie/status?movieId=${m.movieId}">Trạng thái</a>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
