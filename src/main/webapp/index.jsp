<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%
    if (session.getAttribute("loggedInUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return; 
    }
%>
<html>
<head>
    <title>Sảnh Chờ - Cờ Tướng Online</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <jsp:include page="WEB-INF/views/common/header.jsp" />

    <div class="container lobby-container">
        <div class="lobby-panel create-game">
            <h2>Bắt đầu ván cờ mới</h2>
            <p>Tạo một phòng chơi và mời bạn bè cùng tham gia!</p>
            <a href="${pageContext.request.contextPath}/startGame" class="btn">Tạo Ván Mới</a>
        </div>

        <div class="lobby-panel join-game">
            <h2>Tham gia ván cờ</h2>
            <form action="${pageContext.request.contextPath}/joinGame" method="POST" class="join-form">
                <div class="form-group" style="margin:0; flex-grow:1;">
                    <input type="text" name="gameId" placeholder="Nhập mã phòng..." required>
                </div>
                <button type="submit" class="btn">Vào Phòng</button>
            </form>
        </div>
        
        <c:if test="${not empty param.error or not empty errorMessage}">
             <div class="error-message">
                 <c:choose>
                    <c:when test="${param.error == 'room_full_or_invalid'}">Phòng không tồn tại hoặc đã đủ người chơi.</c:when>
                    <c:otherwise>${errorMessage}</c:otherwise>
                 </c:choose>
             </div>
        </c:if>
    </div>
</body>
</html>