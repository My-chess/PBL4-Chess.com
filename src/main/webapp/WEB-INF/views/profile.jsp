<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head>
    <title>Thông Tin Cá Nhân - ${loggedInUser.username}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <jsp:include page="/WEB-INF/views/common/header.jsp" />
    
    <div class="container" style="max-width: 600px;">
        <h1>Thông Tin Cá Nhân</h1>
        <div class="profile-details" style="text-align: left; font-size: 18px; line-height: 2;">
            <p><strong>Tên người dùng:</strong> ${loggedInUser.username}</p>
            <p><strong>Email:</strong> ${loggedInUser.email}</p>
            <p><strong>ELO:</strong> ${loggedInUser.elo}</p>
            <p><strong>Thắng:</strong> ${loggedInUser.winCount}</p>
            <p><strong>Thua:</strong> ${loggedInUser.loseCount}</p>
            <p><strong>Hòa:</strong> ${loggedInUser.drawCount}</p>
        </div>
    </div>
</body>
</html>