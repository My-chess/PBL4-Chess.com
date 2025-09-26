<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %> <%-- Đảm bảo bạn có dòng này --%>
<html>
<head>
    <title>Đăng Ký - Cờ Tướng Online</title>
    <%-- Sử dụng c:url để tạo đường dẫn động --%>
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body>
    <div class="container">
        <h1>Tạo Tài Khoản Mới</h1>
        <%-- Sửa thuộc tính action của form --%>
        <form action="<c:url value='/register'/>" method="POST">
            <div class="form-group">
                <label for="username">Tên hiển thị</label>
                <input type="text" id="username" name="username" required>
            </div>
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required>
            </div>
            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <input type="password" id="password" name="password" required>
            </div>
            <button type="submit" class="btn">Đăng Ký</button>
        </form>

        <c:if test="${not empty errorMessage}">
            <div class="error-message">${errorMessage}</div>
        </c:if>

        <div class="form-link">
            Đã có tài khoản? <a href="<c:url value='/login.jsp'/>">Đăng nhập ngay</a>
        </div>
    </div>
</body>
</html>