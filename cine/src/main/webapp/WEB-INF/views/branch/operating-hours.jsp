<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="pageTitle" value="Giờ mở cửa"/>
<c:set var="nav" value="branches"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<a class="back" href="${ctx}/manage/branches">&larr; Danh sách chi nhánh</a>
<h1 class="page-title">Giờ mở cửa</h1>
<p class="page-sub"><c:out value="${branch.name}"/>. Giờ đóng nhỏ hơn giờ mở nghĩa là đóng cửa sau nửa đêm (tối đa 06:00).</p>

<form method="post" action="${ctx}/manage/branches/hours" id="hoursForm">
    <input type="hidden" name="branchId" value="${branch.branchId}">
    <div class="table-wrap" style="max-width:720px">
        <table class="hours-table">
            <thead><tr><th>Ngày</th><th>Nghỉ</th><th>Mở cửa</th><th>Đóng cửa</th><th></th></tr></thead>
            <tbody>
            <c:forEach var="h" items="${week}">
                <tr data-day="${h.dayOfWeek}">
                    <td><strong>${h.dayName}</strong></td>
                    <td><input type="checkbox" class="inline-check closed-toggle" name="closed_${h.dayOfWeek}"
                               aria-label="${h.dayName} nghỉ" ${h.closed ? 'checked' : ''}></td>
                    <td><input type="time" name="open_${h.dayOfWeek}" value="${h.openTime}" aria-label="Giờ mở ${h.dayName}" ${h.closed ? 'disabled' : ''}></td>
                    <td><input type="time" name="close_${h.dayOfWeek}" value="${h.closeTime}" aria-label="Giờ đóng ${h.dayName}" ${h.closed ? 'disabled' : ''}></td>
                    <td><span class="overnight" ${h.overnight ? '' : 'hidden'}>Qua đêm</span></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <div class="form-actions">
        <button type="submit" class="btn btn-primary">Lưu giờ mở cửa</button>
        <button type="button" class="btn" id="copyMonday">Áp dụng giờ Thứ Hai cho cả tuần</button>
        <a class="btn btn-quiet" href="${ctx}/manage/branches">Huỷ</a>
    </div>
</form>

<script>
(function () {
    var rows = Array.prototype.slice.call(document.querySelectorAll('tr[data-day]'));
    function inputs(row) { return row.querySelectorAll('input[type="time"]'); }
    function refresh(row) {
        var closed = row.querySelector('.closed-toggle').checked;
        var t = inputs(row);
        t[0].disabled = closed; t[1].disabled = closed;
        var overnight = !closed && t[0].value && t[1].value && t[1].value < t[0].value;
        row.querySelector('.overnight').hidden = !overnight;
    }
    rows.forEach(function (row) {
        row.querySelector('.closed-toggle').addEventListener('change', function () { refresh(row); });
        inputs(row).forEach(function (i) { i.addEventListener('input', function () { refresh(row); }); });
    });
    document.getElementById('copyMonday').addEventListener('click', function () {
        var mon = rows[0], src = inputs(mon), closed = mon.querySelector('.closed-toggle').checked;
        rows.slice(1).forEach(function (row) {
            row.querySelector('.closed-toggle').checked = closed;
            var t = inputs(row);
            t[0].value = src[0].value; t[1].value = src[1].value;
            refresh(row);
        });
    });
})();
</script>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
