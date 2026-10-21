<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="isNew" value="${genre.genreId == 0}"/>
<c:set var="pageTitle" value="${isNew ? 'Thêm thể loại' : 'Sửa thể loại'}"/>
<c:set var="nav" value="genres"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<c:set var="backUrl" value="${ctx}/manage/genres${isNew ? '' : '?id='}${isNew ? '' : genre.genreId}"/>
<a class="back" href="${backUrl}">&larr; Thể loại</a>
<h1 class="page-title">${isNew ? 'Thêm thể loại' : 'Sửa thể loại'}</h1>

<form class="form-card" method="post" action="${ctx}/manage/genres">
    <input type="hidden" name="action" value="save">
    <c:if test="${not isNew}"><input type="hidden" name="id" value="${genre.genreId}"></c:if>
    <div class="field">
        <label for="name" class="required">Tên thể loại</label>
        <input type="text" id="name" name="name" maxlength="50" required value="${fn:escapeXml(genre.name)}" placeholder="Hành động"
               aria-describedby="nameError" ${not empty errors ? 'autofocus' : ''}>
        <p class="field-error" id="nameError" aria-live="polite" hidden></p>
    </div>
    <div class="field">
        <label for="description">Mô tả</label>
        <input type="text" id="description" name="description" maxlength="255" value="${fn:escapeXml(genre.description)}">
    </div>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">${isNew ? 'Thêm thể loại' : 'Lưu thay đổi'}</button>
        <a class="btn btn-quiet" href="${backUrl}">Huỷ</a>
    </div>
</form>

<%-- Tên các thể loại khác, để báo trùng ngay khi gõ. Server vẫn kiểm tra lại khi lưu. --%>
<ul id="existingGenres" hidden>
    <c:forEach var="eg" items="${existingGenres}">
        <c:if test="${eg.genreId != genre.genreId}"><li data-name="${fn:escapeXml(eg.name)}"></li></c:if>
    </c:forEach>
</ul>
<script src="${ctx}/assets/js/genre-form-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
