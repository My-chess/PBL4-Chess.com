<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head>
    <title>Đăng Nhập - Cờ Tướng Online</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="container">
        <h1>Đăng Nhập</h1>
        
        <c:if test="${param.success == 'true'}">
            <div class="success-message">
                Đăng ký thành công! Vui lòng đăng nhập.
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="POST">
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required>
            </div>
            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <input type="password" id="password" name="password" required>
            </div>
            <button type="submit" class="btn">Đăng Nhập</button>
        </form>
        
        <c:if test="${not empty errorMessage}">
            <div class="error-message">${errorMessage}</div>
        </c:if>

        <div class="form-link">
            Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register.jsp">Tạo tài khoản</a>
        </div>
    </div>
</body>
</html>