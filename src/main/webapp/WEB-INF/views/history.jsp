<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head>
    <title>Lịch Sử Đấu - ${loggedInUser.username}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .history-table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        .history-table th, .history-table td { padding: 12px 15px; border-bottom: 1px solid #3e3e3e; }
        .history-table th { background-color: #3e3e3e; text-align: left; }
        .history-table tr:nth-child(even) { background-color: #2c2c2c; }
        .result-win { color: #4CAF50; }
        .result-loss { color: #f44336; }
    </style>
</head>
<body>
    <jsp:include page="/WEB-INF/views/common/header.jsp" />
    
    <div class="container" style="max-width: 800px;">
        <h1>Lịch Sử Đấu</h1>
        <%-- Dữ liệu giả lập - bạn sẽ thay thế bằng dữ liệu thật từ Servlet --%>
        <table class="history-table">
            <thead>
                <tr>
                    <th>Đối thủ</th>
                    <th>Kết quả</th>
                    <th>Thay đổi ELO</th>
                    <th>Ngày</th>
                </tr>
            </thead>
            <tbody>
                <%-- TODO: Dùng JSTL <c:forEach> để lặp qua danh sách lịch sử đấu từ request --%>
                <tr>
                    <td>PlayerX</td>
                    <td class="result-win">Thắng</td>
                    <td>+15</td>
                    <td>25/09/2025</td>
                </tr>
                <tr>
                    <td>PlayerY</td>
                    <td class="result-loss">Thua</td>
                    <td>-15</td>
                    <td>24/09/2025</td>
                </tr>
            </tbody>
        </table>
    </div>
</body>
</html>