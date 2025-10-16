<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %> <%-- Khai báo để dùng JSTL --%>

<html>
<head>
    <title>Danh sách phòng chờ</title>
    <%-- Bạn có thể link đến file CSS chung của mình ở đây --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        /* CSS riêng cho trang lobby */
        .lobby-container {
            width: 80%;
            margin: 40px auto;
            background-color: #2c2c2e;
            padding: 20px;
            border-radius: 8px;
        }
        .lobby-table {
            width: 100%;
            border-collapse: collapse;
            color: #f0f0f0;
        }
        .lobby-table th, .lobby-table td {
            padding: 12px 15px;
            border-bottom: 1px solid #444;
            text-align: left;
        }
        .lobby-table th {
            background-color: #3a3a3c;
        }
        .lobby-table tr:hover {
            background-color: #3f3f41;
        }
        .join-button {
            padding: 8px 16px;
            background-color: #4CAF50;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-weight: bold;
        }
        .join-button:hover {
            background-color: #45a049;
        }
    </style>
</head>
<body>
    <%-- Nhúng header chung --%>
    <c:import url="/WEB-INF/views/common/header.jsp" />

    <div class="lobby-container">
        <h1>Tìm trận đấu</h1>
        <hr>
        <table class="lobby-table">
            <thead>
                <tr>
                    <th>Người tạo phòng</th>
                    <th>Elo</th>
                    <th>Hành động</th>
                </tr>
            </thead>
            <tbody>
                <%-- Dùng JSTL để lặp qua danh sách waitingMatches --%>
                <c:forEach items="${waitingMatches}" var="match">
                    <tr>
                        <td><c:out value="${match.player1.displayName}" /></td>
                        <td><c:out value="${match.player1.elo}" /></td>
                        <td>
                            <%-- Form này sẽ gửi yêu cầu POST đến JoinGameServlet --%>
                            <form action="${pageContext.request.contextPath}/joinGame" method="POST">
                                <input type="hidden" name="gameId" value="${match.matchId}" />
                                <button type="submit" class="join-button">Tham gia</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                
                <%-- Hiển thị thông báo nếu không có phòng nào --%>
                <c:if test="${empty waitingMatches}">
                    <tr>
                        <td colspan="3" style="text-align: center;">Không có phòng nào đang chờ. Hãy tạo một phòng mới!</td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>

</body>
</html>