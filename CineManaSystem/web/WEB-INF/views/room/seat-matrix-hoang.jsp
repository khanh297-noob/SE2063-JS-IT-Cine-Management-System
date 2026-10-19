<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="room" value="${matrix.room}"/>
<c:set var="pageTitle" value="Sơ đồ ghế"/>
<c:set var="nav" value="rooms"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/rooms?branchId=${room.branchId}">&larr; Phòng chiếu</a>
<h1 class="page-title"><c:out value="${room.roomName}"/></h1>
<p class="page-sub"><c:out value="${matrix.branchName}"/></p>
<nav class="tabs" aria-label="Phòng">
    <a href="${ctx}/manage/rooms?action=edit&id=${room.roomId}">Thông tin</a>
    <a class="current" href="${ctx}/manage/rooms/seats?roomId=${room.roomId}">Sơ đồ ghế</a>
</nav>

<c:if test="${matrix.structureLocked}">
    <div class="notice notice-info">
        Phòng còn ${matrix.upcomingShowtimes} suất chiếu sắp tới. Bạn chỉ đổi được loại ghế hoặc đánh dấu ghế hỏng,
        không thêm, bớt ghế hay đổi kích thước.
    </div>
</c:if>
<c:if test="${empty matrix.seatTypes}">
    <div class="notice notice-error">Chưa có loại ghế nào trong bảng SeatTypes. Chạy file database/02_seed.sql.</div>
</c:if>

<div class="notice notice-error matrix-issues" id="layoutIssues" role="alert" hidden>
    Sơ đồ chưa hợp lệ (các ô viền đỏ):
    <ul id="layoutIssueList"></ul>
</div>

<div class="matrix-layout" id="matrixEditor"
     data-rows="${matrix.rows}" data-cols="${matrix.columns}"
     data-max-rows="${maxRows}" data-max-cols="${maxCols}"
     data-locked="${matrix.structureLocked}" data-layout="${fn:escapeXml(matrix.layout)}">
    <div class="matrix-stage">
        <div class="screen"></div>
        <p class="screen-label">Màn hình</p>
        <div class="grid-wrap"><div class="seat-grid" id="seatGrid" role="grid" aria-label="Sơ đồ ghế"></div></div>
    </div>

    <aside class="palette">
        <h2>Tô ghế</h2>
        <c:forEach var="t" items="${matrix.seatTypes}" varStatus="st">
            <button type="button" class="brush" data-brush="type" data-type="${t.seatTypeId}"
                    data-color="${fn:escapeXml(t.color)}" data-sweetbox="${t.sweetbox}"
                    data-name="${fn:escapeXml(t.name)}" aria-pressed="${st.first}">
                <span class="swatch" style="background:${fn:escapeXml(t.color)}"></span>
                <c:out value="${t.name}"/>
                <span class="count" data-count="${t.seatTypeId}">0</span>
            </button>
        </c:forEach>
        <button type="button" class="brush" data-brush="broken" aria-pressed="false">
            <span class="swatch cell broken" style="background-color:#7d8299"></span>Ghế hỏng
            <span class="count" data-count="broken">0</span>
        </button>
        <button type="button" class="brush" data-brush="aisle" aria-pressed="false">
            <span class="swatch cell aisle"></span>Lối đi
        </button>
        <hr>
        <h2>Tô theo vùng</h2>
        <div class="region-form">
            <div class="region-row">
                <span class="region-label">Hàng</span>
                <select id="rowFrom" aria-label="Từ hàng"></select>
                <span>đến</span>
                <select id="rowTo" aria-label="Đến hàng"></select>
            </div>
            <div class="region-row">
                <span class="region-label">Cột</span>
                <input type="number" id="colFrom" min="1" value="1" aria-label="Từ cột">
                <span>đến</span>
                <input type="number" id="colTo" min="1" value="1" aria-label="Đến cột">
            </div>
            <div class="region-row">
                <label class="region-label" for="regionBrush">Loại</label>
                <select id="regionBrush">
                    <c:forEach var="t" items="${matrix.seatTypes}">
                        <option value="type:${t.seatTypeId}"><c:out value="${t.name}"/></option>
                    </c:forEach>
                    <option value="broken">Ghế hỏng</option>
                    <option value="aisle" ${matrix.structureLocked ? 'disabled' : ''}>Lối đi</option>
                </select>
            </div>
            <button type="button" class="btn btn-sm" id="regionApply">Áp dụng cho vùng</button>
            <p class="meta region-msg" id="regionMsg" aria-live="polite"></p>
        </div>
        <hr>
        <div class="size-form">
            <div><label for="rowsInput">Số hàng</label>
                <input type="number" id="rowsInput" min="1" max="${maxRows}" value="${matrix.rows}"></div>
            <div><label for="colsInput">Số cột</label>
                <input type="number" id="colsInput" min="1" max="${maxCols}" value="${matrix.columns}"></div>
            <button type="button" class="btn btn-sm" id="resizeBtn">Đổi kích thước</button>
        </div>
        <hr>
        <p><strong id="totalSeats">0</strong> ghế dùng được</p>
        <p class="meta">Nhấn giữ và kéo để tô nhiều ô. Nhấn chữ cái đầu hàng hoặc số đầu cột để tô cả hàng, cả cột.</p>
        <p class="meta">Lối đi phải liền nhau và thông ra ít nhất một góc phòng. Ghế Sweetbox phải đi theo cặp.</p>
    </aside>
</div>

<form method="post" action="${ctx}/manage/rooms/seats" id="matrixForm">
    <input type="hidden" name="roomId" value="${room.roomId}">
    <input type="hidden" name="rows" id="rowsField">
    <input type="hidden" name="cols" id="colsField">
    <input type="hidden" name="layout" id="layoutField">
    <div class="savebar">
        <div class="savebar-inner">
            <span class="pending" id="pending" aria-live="polite">Chưa có thay đổi</span>
            <a class="btn btn-quiet" href="${ctx}/manage/rooms/seats?roomId=${room.roomId}">Huỷ thay đổi</a>
            <button type="submit" class="btn btn-primary" ${empty matrix.seatTypes ? 'disabled' : ''}>Lưu sơ đồ ghế</button>
        </div>
    </div>
</form>

<script src="${ctx}/assets/js/seat-matrix-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
