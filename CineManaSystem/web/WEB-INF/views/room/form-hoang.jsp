<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<c:set var="isNew" value="${room.roomId == 0}"/>
<c:set var="pageTitle" value="${isNew ? 'Thêm phòng' : 'Sửa phòng'}"/>
<c:set var="nav" value="rooms"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/rooms${isNew ? '' : '?branchId='.concat(room.branchId)}">&larr; Phòng chiếu</a>
<h1 class="page-title">${isNew ? 'Thêm phòng' : 'Sửa phòng'}</h1>
<c:if test="${not isNew}">
    <nav class="tabs" aria-label="Phòng">
        <a class="current" href="${ctx}/manage/rooms?action=edit&id=${room.roomId}">Thông tin</a>
        <a href="${ctx}/manage/rooms/seats?roomId=${room.roomId}">Sơ đồ ghế</a>
    </nav>
</c:if>

<form class="form-card" method="post" action="${ctx}/manage/rooms">
    <input type="hidden" name="action" value="save">
    <c:if test="${not isNew}"><input type="hidden" name="id" value="${room.roomId}"></c:if>

    <div class="field">
        <label for="branchId" class="required">Chi nhánh</label>
        <c:choose>
            <c:when test="${isNew}">
                <select id="branchId" name="branchId" required>
                    <option value="">Chọn chi nhánh</option>
                    <c:forEach var="b" items="${branches}">
                        <option value="${b.branchId}" ${b.branchId == room.branchId ? 'selected' : ''}><c:out value="${b.name}"/></option>
                    </c:forEach>
                </select>
                <div class="hint">Chỉ chi nhánh đang hoạt động. Không đổi được sau khi tạo.</div>
            </c:when>
            <c:otherwise>
                <c:forEach var="b" items="${allBranches}">
                    <c:if test="${b.branchId == room.branchId}"><input type="text" id="branchId" value="${fn:escapeXml(b.name)}" disabled></c:if>
                </c:forEach>
                <input type="hidden" name="branchId" value="${room.branchId}">
            </c:otherwise>
        </c:choose>
    </div>
    <div class="field-row">
        <div class="field">
            <label for="roomName" class="required">Tên phòng</label>
            <input type="text" id="roomName" name="roomName" maxlength="50" required value="${fn:escapeXml(room.roomName)}" placeholder="Phòng 1">
        </div>
        <div class="field">
            <label for="roomTypeId" class="required">Loại phòng</label>
            <select id="roomTypeId" name="roomTypeId" required>
                <option value="">Chọn loại phòng</option>
                <c:forEach var="t" items="${roomTypes}">
                    <c:set var="isCurrent" value="${t.roomTypeId == room.roomTypeId}"/>
                    <c:if test="${t.active or isCurrent}">
                        <option value="${t.roomTypeId}" ${isCurrent ? 'selected' : ''}>
                            <c:out value="${t.name}"/>${t.active ? '' : ' (ngưng dùng)'}
                        </option>
                    </c:if>
                </c:forEach>
            </select>
        </div>
    </div>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">${isNew ? 'Tạo phòng và xếp ghế' : 'Lưu thay đổi'}</button>
        <a class="btn btn-quiet" href="${ctx}/manage/rooms">Huỷ</a>
    </div>
</form>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
