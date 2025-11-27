<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %> <%-- Khai báo để dùng JSTL --%>

<html>
<head>
    <title>Danh sách phòng chờ</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    
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
                        
                        <%-- === THAY ĐỔI CỐT LÕI BẮT ĐẦU TỪ ĐÂY === --%>
                        <c:choose>
                            <%-- Trường hợp phòng do người chơi 1 (Đỏ) tạo --%>
                            <c:when test="${not empty match.player1}">
                                <td><c:out value="${match.player1.displayName}" /> (Đỏ)</td>
                                <td><c:out value="${match.player1.elo}" /></td>
                            </c:when>
                            
                            <%-- Trường hợp phòng do người chơi 2 (Đen) tạo --%>
                            <c:when test="${not empty match.player2}">
                                <td><c:out value="${match.player2.displayName}" /> (Đen)</td>
                                <td><c:out value="${match.player2.elo}" /></td>
                            </c:when>
                            
                            <%-- (Phòng bị lỗi) --%>
                            <c:otherwise>
                                <td>[Phòng lỗi]</td>
                                <td>N/A</td>
                            </c:otherwise>
                        </c:choose>
                        <%-- === KẾT THÚC THAY ĐỔI === --%>

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