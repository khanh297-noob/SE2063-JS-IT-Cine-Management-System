<c:set var="pageTitle" value="Lỗi"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="card narrow center">
    <h1 class="page-title">Rất tiếc!</h1>
    <p class="lead">
        <c:choose>
            <c:when test="${not empty errorMessage}">${fn:escapeXml(errorMessage)}</c:when>
            <c:when test="${pageContext.errorData.statusCode == 404}">Không tìm thấy trang bạn yêu cầu.</c:when>
            <c:otherwise>Đã có lỗi xảy ra. Vui lòng thử lại sau. (MSG53)</c:otherwise>
        </c:choose>
    </p>
    <c:if test="${not empty errorDetail}"><p class="muted small mono">${fn:escapeXml(errorDetail)}</p></c:if>
    <a class="btn btn-primary" href="${ctx}/">Về trang chủ</a>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
