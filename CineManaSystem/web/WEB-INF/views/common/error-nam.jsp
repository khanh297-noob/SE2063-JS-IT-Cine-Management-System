<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo - Thông báo</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<header class="topbar">
    <div class="brand"><span class="logo">CG</span> CineGo</div>
    <a href="javascript:history.back()">&larr; Quay lại</a>
</header>
<div class="container" style="max-width:560px">
    <div class="card center">
        <div style="font-size:42px">&#9888;</div>
        <h2>Không thể thực hiện</h2>
        <p class="text-muted"><c:out value="${errorMsg}" default="Something went wrong. Please try again later."/></p>
        <a class="btn btn-primary" href="${ctx}/">Về trang chủ</a>
    </div>
</div>
</body>
</html>
