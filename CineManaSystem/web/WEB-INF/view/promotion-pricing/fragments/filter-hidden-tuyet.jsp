<%@ page pageEncoding="UTF-8" %>
<!-- Giu lai bo loc (chi nhanh, phim, khoang ngay) de sau khi luu van o dung danh sach dang xem -->
<c:if test="${not empty selectedBranchId}"><input type="hidden" name="branchId" value="${selectedBranchId}"></c:if>
<c:if test="${not empty selectedMovieId}"><input type="hidden" name="movieId" value="${selectedMovieId}"></c:if>
<input type="hidden" name="fromDate" value="<c:out value='${fromDate}'/>">
<input type="hidden" name="toDate" value="<c:out value='${toDate}'/>">
