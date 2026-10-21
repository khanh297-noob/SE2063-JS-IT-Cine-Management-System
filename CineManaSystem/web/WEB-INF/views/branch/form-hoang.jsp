<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="isNew" value="${branch.branchId == 0}"/>
<c:set var="pageTitle" value="${isNew ? 'Thêm chi nhánh' : 'Sửa chi nhánh'}"/>
<c:set var="nav" value="branches"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/branch">&larr; Danh sách chi nhánh</a>
<h1 class="page-title">${isNew ? 'Thêm chi nhánh' : 'Sửa chi nhánh'}</h1>
<p class="page-sub">
    <c:choose>
        <c:when test="${isNew}">Chi nhánh mới được tạo sẵn giờ mở cửa 09:00 - 23:00, bạn sửa lại sau.</c:when>
        <c:otherwise><c:out value="${branch.name}"/></c:otherwise>
    </c:choose>
</p>

<form class="form-card" method="post" action="${ctx}/manage/branch">
    <input type="hidden" name="action" value="save">
    <c:if test="${not isNew}"><input type="hidden" name="id" value="${branch.branchId}"></c:if>

    <div class="field">
        <label for="name" class="required">Tên chi nhánh</label>
        <input type="text" id="name" name="name" maxlength="150" required value="${fn:escapeXml(branch.name)}" placeholder="CineGo Cầu Giấy">
    </div>
    <div class="field">
        <label for="address" class="required">Địa chỉ</label>
        <input type="text" id="address" name="address" maxlength="255" required value="${fn:escapeXml(branch.address)}" placeholder="Số nhà, đường, phường/quận">
    </div>
    <div class="field-row">
        <div class="field">
            <label for="city" class="required">Thành phố</label>
            <input type="text" id="city" name="city" maxlength="100" required list="cities" value="${fn:escapeXml(branch.city)}">
            <datalist id="cities">
                <option value="Hà Nội"><option value="TP. Hồ Chí Minh"><option value="Đà Nẵng">
                <option value="Hải Phòng"><option value="Cần Thơ">
            </datalist>
        </div>
        <div class="field">
            <label for="hotline" class="required">Hotline</label>
            <input type="tel" id="hotline" name="hotline" maxlength="20" required value="${fn:escapeXml(branch.hotline)}" placeholder="0241234567 hoặc 19001234">
        </div>
    </div>
    <div class="field">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" maxlength="150" value="${fn:escapeXml(branch.email)}">
        <div class="hint">Không bắt buộc.</div>
    </div>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">${isNew ? 'Thêm chi nhánh' : 'Lưu thay đổi'}</button>
        <a class="btn btn-quiet" href="${ctx}/manage/branch">Huỷ</a>
    </div>
</form>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
