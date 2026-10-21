<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="isNew" value="${movie.movieId == 0}"/>
<c:set var="pageTitle" value="${isNew ? 'Thêm phim' : 'Sửa phim'}"/>
<c:set var="nav" value="movies"/>
<c:set var="movieTab" value="info"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/movie">&larr; Danh sách phim</a>
<h1 class="page-title"><c:out value="${isNew ? 'Thêm phim' : movie.title}"/></h1>
<c:choose>
    <c:when test="${isNew}"><p class="page-sub">Phim mới ở trạng thái Sắp chiếu. Sau khi lưu, bạn thêm poster và chọn chi nhánh.</p></c:when>
    <c:otherwise><%@ include file="/WEB-INF/views/common/movie-tabs-hoang.jspf" %></c:otherwise>
</c:choose>

<form class="form-card" method="post" action="${ctx}/manage/movie">
    <input type="hidden" name="action" value="save">
    <c:if test="${not isNew}"><input type="hidden" name="id" value="${movie.movieId}"></c:if>

    <div class="field">
        <label for="title" class="required">Tên phim</label>
        <input type="text" id="title" name="title" maxlength="200" required value="${fn:escapeXml(movie.title)}">
    </div>
    <div class="field-row">
        <div class="field">
            <label for="duration" class="required">Thời lượng (phút)</label>
            <input type="number" id="duration" name="duration" min="1" max="600" required value="${movie.duration}">
        </div>
        <div class="field">
            <label for="releaseDate" class="required">Ngày khởi chiếu</label>
            <input type="date" id="releaseDate" name="releaseDate" required value="${movie.releaseDate}">
        </div>
        <div class="field">
            <label for="ageRating" class="required">Phân loại tuổi</label>
            <select id="ageRating" name="ageRating" required>
                <option value="">Chọn</option>
                <c:forEach var="r" items="${ageRatings}">
                    <option value="${r}" ${r == movie.ageRating ? 'selected' : ''}>${r}</option>
                </c:forEach>
            </select>
        </div>
    </div>
    <div class="field">
        <span class="label required">Thể loại</span>
        <c:choose>
            <c:when test="${empty genres}">
                <p class="meta">Chưa có thể loại nào. <a href="${ctx}/manage/genres?action=create">Thêm thể loại</a> trước.</p>
            </c:when>
            <c:otherwise>
                <div class="check-grid">
                    <c:forEach var="g" items="${genres}">
                        <label class="check-pill">
                            <input type="checkbox" name="genreIds" value="${g.genreId}" ${selectedGenres.contains(g.genreId) ? 'checked' : ''}>
                            <c:out value="${g.name}"/>
                        </label>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="field-row">
        <div class="field">
            <label for="director">Đạo diễn</label>
            <input type="text" id="director" name="director" maxlength="150" value="${fn:escapeXml(movie.director)}">
        </div>
        <div class="field">
            <label for="language">Ngôn ngữ</label>
            <input type="text" id="language" name="language" maxlength="50" value="${fn:escapeXml(movie.language)}" placeholder="Tiếng Anh, phụ đề Việt">
        </div>
    </div>
    <div class="field">
        <label for="castInput">Diễn viên</label>
        <div class="cast-entry">
            <input type="text" id="castInput" maxlength="100" autocomplete="off"
                   placeholder="Nhập tên diễn viên rồi nhấn Enter" aria-describedby="castHint castError">
            <button type="button" class="btn" id="castAdd">Thêm</button>
        </div>
        <p class="field-error" id="castError" aria-live="polite" hidden></p>
        <%-- Danh sách card. Server render sẵn để form hiển thị đúng khi tải lại sau lỗi. --%>
        <ul class="cast-cards" id="castCards" aria-label="Diễn viên đã thêm">
            <c:forEach var="actor" items="${movie.castList}">
                <li class="cast-card" data-name="${fn:escapeXml(actor)}">
                    <span class="cast-card-name"><c:out value="${actor}"/></span>
                    <button type="button" class="cast-card-remove" aria-label="Xoá ${fn:escapeXml(actor)}">&times;</button>
                </li>
            </c:forEach>
        </ul>
        <input type="hidden" id="castMembers" name="castMembers" value="${fn:escapeXml(movie.castMembers)}">
        <p class="hint" id="castHint">Nhấn Enter, dấu phẩy hoặc nút Thêm để thêm từng diễn viên. Bấm &times; trên thẻ để xoá.</p>
    </div>
    <div class="field">
        <label for="description">Nội dung</label>
        <textarea id="description" name="description"><c:out value="${movie.description}"/></textarea>
    </div>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">${isNew ? 'Thêm phim' : 'Lưu thay đổi'}</button>
        <a class="btn btn-quiet" href="${ctx}/manage/movie">Huỷ</a>
    </div>
</form>

<c:if test="${not isNew}">
    <form method="post" action="${ctx}/manage/movie" style="margin-top:20px">
        <input type="hidden" name="action" value="delete">
        <input type="hidden" name="id" value="${movie.movieId}">
        <button class="btn btn-danger" type="submit"
                data-confirm="Xoá phim ${fn:escapeXml(movie.title)} cùng poster, trailer và cấu hình chi nhánh?">Xoá phim</button>
        <span class="meta">Chỉ xoá được phim chưa từng có suất chiếu.</span>
    </form>
    <script src="${ctx}/assets/js/confirm-hoang.js?v=${assetVer}"></script>
</c:if>
<script src="${ctx}/assets/js/movie-cast-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
