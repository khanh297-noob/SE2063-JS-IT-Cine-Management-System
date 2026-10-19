<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<c:set var="isNew" value="${roomType.roomTypeId == 0}"/>
<c:set var="pageTitle" value="${isNew ? 'Thêm loại phòng' : 'Sửa loại phòng'}"/>
<c:set var="nav" value="room-types"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/room-types">&larr; Loại phòng</a>
<h1 class="page-title">${isNew ? 'Thêm loại phòng' : 'Sửa loại phòng'}</h1>
<p class="page-sub">Ví dụ: 2D, 3D, IMAX, 4DX.</p>

<form class="form-card" method="post" action="${ctx}/manage/room-types">
    <input type="hidden" name="action" value="save">
    <c:if test="${not isNew}"><input type="hidden" name="id" value="${roomType.roomTypeId}"></c:if>
    <div class="field-row">
        <div class="field">
            <label for="name" class="required">Tên loại phòng</label>
            <input type="text" id="name" name="name" maxlength="50" required value="${fn:escapeXml(roomType.name)}">
        </div>
        <div class="field">
            <label for="surcharge" class="required">Phụ thu (đ)</label>
            <input type="number" id="surcharge" name="surcharge" min="0" max="1000000" step="1000" required
                   value="${empty roomType.surcharge ? '' : roomType.surcharge.stripTrailingZeros().toPlainString()}">
            <div class="hint">0 nếu không phụ thu.</div>
        </div>
    </div>
    <div class="field">
        <label for="description">Mô tả</label>
        <input type="text" id="description" name="description" maxlength="255" value="${fn:escapeXml(roomType.description)}"
               placeholder="Đặc điểm khách nhìn thấy, vd: màn hình cong, ghế rung">
    </div>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">${isNew ? 'Thêm loại phòng' : 'Lưu thay đổi'}</button>
        <a class="btn btn-quiet" href="${ctx}/manage/room-types">Huỷ</a>
    </div>
</form>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
