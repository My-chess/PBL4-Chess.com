<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Model.BEAN.UserBEAN" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %> <%-- Dòng này quan trọng --%>
<%
    // Lấy thông tin user từ session (nếu có)
    UserBEAN loggedInUser = (UserBEAN) session.getAttribute("loggedInUser");
    String currentUserId = (loggedInUser != null) ? loggedInUser.getUid() : "guest";
    String username = (loggedInUser != null) ? loggedInUser.getUsername() : "Khách";
    String playerColor = (String) session.getAttribute("ai_player_color"); // Màu quân người chơi chọn
    String aiColor = "Red".equals(playerColor) ? "Black" : "Red"; // Màu quân của máy
    String difficulty = (String) session.getAttribute("ai_difficulty"); // Độ khó
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chơi với Máy - <%= username %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/game.css">
</head>
<body>
    <jsp:include page="/WEB-INF/views/common/header.jsp" />

    <div class="game-container">
        <div class="main-content">
            <%-- Phần thông tin máy (Top Panel) --%>
            <div class="player-info-panel top">
                <div class="captured-pieces" id="captured-for-ai"></div>
                <div class="player-details">
                    <span class="player-name" id="player-ai-name">Máy (<%= difficulty %>)</span> 
                </div>
            </div>

            <%-- Bàn cờ --%>
            <div class="board-frame">
                <div id="board">
                    <%-- JavaScript sẽ vẽ bàn cờ và quân cờ ở đây --%>
                </div>
            </div>

            <%-- Phần thông tin người chơi (Bottom Panel) --%>
            <div class="player-info-panel bottom">
                <div class="captured-pieces" id="captured-for-player"></div>
                <div class="player-details">
                    <span class="player-name" id="player-name"><%= username %></span>
                 </div> 
            </div>
        </div>

        <%-- Sidebar --%>
        <div class="sidebar">
            <h2>Chơi với Máy</h2>
            <h3 style="color: var(--text-light); text-align: center; margin-top: -10px;">Độ khó: <%= difficulty %></h3>

            <div id="turn-indicator">
                Lượt đi: <span id="turn-color" class="<%= "Red".equals(playerColor) ? "red" : "black" %>">
                    <%= "Red".equals(playerColor) ? "Người chơi" : "Máy" %>
                </span>
            </div>
            <%-- Các nút hành động: đầu hàng --%>
            <div class="game-actions" id="game-actions-panel">
                <button id="resign-btn" class="btn" style="background-color: var(--accent-red);">Đầu hàng</button>
            </div>
        </div>
    </div>

    <%-- Modal thông báo kết thúc game --%>
    <div id="gameOverModal" class="modal-overlay">
        <div class="modal-content">
            <h2>Kết quả ván cờ</h2>
            <p id="gameOverMessage"></p>
            <a href="${pageContext.request.contextPath}/index.jsp" class="modal-button">Về trang chủ</a>
        </div>
    </div>

    <%-- Biến JS cho client --%>
    <script>
        const contextPath = "${pageContext.request.contextPath}";
        const currentUserId = "<%= currentUserId %>";
        const playerColor = "<%= playerColor %>"; // Màu quân của người chơi
        const aiColor = "<%= aiColor %>";       // Màu quân của máy
        const initialBoardState = JSON.parse(sessionStorage.getItem('ai_initial_board'));
    </script>

    <%-- Import JS --%>
    <script src="${pageContext.request.contextPath}/js/chess-logic.js"></script>
    <script src="${pageContext.request.contextPath}/js/ai_game.js"></script>
</body>
</html>