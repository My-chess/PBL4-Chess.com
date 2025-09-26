<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<style>
    .main-header {
        position: fixed; top: 0; left: 0; width: 100%;
        background-color: #2c2c2c; padding: 10px 40px;
        box-shadow: 0 2px 5px rgba(0,0,0,0.3);
        display: flex; justify-content: space-between; align-items: center;
        box-sizing: border-box; z-index: 1000;
    }
    .main-header .logo {
        font-size: 24px; font-weight: bold; color: white; text-decoration: none;
    }
    .main-header nav a {
        color: #e0e0e0; text-decoration: none; margin-left: 20px;
        font-size: 16px; transition: color 0.2s;
    }
    .main-header nav a:hover { color: #4CAF50; }
    .user-info { display: flex; align-items: center; }
    .user-info span { margin-right: 20px; }
</style>

<header class="main-header">
    <a href="${pageContext.request.contextPath}/index.jsp" class="logo">Cờ Tướng Online</a>
    <div class="user-info">
        <c:if test="${not empty loggedInUser}">
            <span>Chào, <strong>${loggedInUser.username}</strong></span>
            <nav>
                <a href="${pageContext.request.contextPath}/app/profile">Thông tin cá nhân</a>
                <a href="${pageContext.request.contextPath}/app/history">Lịch sử đấu</a>
                <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
            </nav>
        </c:if>
    </div>
</header>
<div style="height: 60px;"></div>