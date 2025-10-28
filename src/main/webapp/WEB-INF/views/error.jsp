<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head>
    <title>Đã có lỗi xảy ra</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .error-container {
            text-align: center;
            padding: 40px 20px;
        }
        .error-details {
            background-color: #2c2c2c;
            border: 1px solid #444;
            padding: 20px;
            margin-top: 20px;
            text-align: left;
            display: inline-block;
            max-width: 800px;
            word-wrap: break-word;
        }
    </style>
</head>
<body>
    <jsp:include page="/WEB-INF/views/common/header.jsp" />

    <div class="container error-container">
        <h1>Rất tiếc, đã có lỗi xảy ra</h1>
        <p>Hệ thống gặp phải một sự cố không mong muốn. Vui lòng thử lại sau.</p>
        
        <%-- Đoạn này sẽ hiển thị thông báo lỗi mà chúng ta đã set trong Servlet --%>
        <c:if test="${not empty errorMessage}">
            <div class="error-details">
                <strong>Thông tin lỗi từ Server:</strong>
                <p>${errorMessage}</p>
            </div>
        </c:if>
    </div>
</body>
</html>