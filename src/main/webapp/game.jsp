<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Model.BEAN.UserBEAN" %> <%-- DÒNG QUAN TRỌNG ĐỂ SỬA LỖI --%>
<%
    // Giả sử user đã được lưu trong session
    UserBEAN loggedInUser = (UserBEAN) session.getAttribute("loggedInUser");
    String currentUserId = (loggedInUser != null) ? loggedInUser.getUid() : "spectator";
%>
<html>
<head>
    <title>Ván Cờ #${param.gameId}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/game.css"> 
</head>
<body>
    <div class="game-container">
        <div class="main-content">
            <!-- Bảng thông tin người chơi Đen (Phía trên) -->
            <div class="player-info-panel top">
                <div class="player-details">
                    <span class="player-name" id="player-black-name">Chờ người chơi...</span>
                    <span class="player-elo" id="player-black-elo"></span>
                </div>
                <div class="timer" id="timer-black">10:00</div>
            </div>

            <!-- Khu vực bàn cờ -->
            <div class="board-frame">
                <div id="board">
                    <!-- JavaScript sẽ vẽ bàn cờ ở đây -->
                </div>
            </div>
            
            <!-- Bảng thông tin người chơi Đỏ (Phía dưới) -->
            <div class="player-info-panel bottom">
                 <div class="player-details">
                    <span class="player-name" id="player-red-name">...</span>
                    <span class="player-elo" id="player-red-elo"></span>
                </div>
                <div class="timer active" id="timer-red">10:00</div>
            </div>
        </div>

        <!-- Cột bên phải cho trạng thái và lịch sử -->
        <div class="sidebar">
            <h2>Ván cờ #${param.gameId}</h2>
            <div id="turn-indicator">
                Lượt đi: <span id="turn-color" class="red">...</span>
            </div>
            <div id="status-message">Đang tải ván cờ...</div>
            <div class="move-history">
                <h3>Lịch sử nước đi</h3>
                <ul id="move-list">
                    <!-- Nước đi sẽ được thêm vào đây bằng JS -->
                </ul>
            </div>
        </div>
    </div>
    
    <script>
        // Truyền các biến từ JSP sang JavaScript
        const gameId = "${param.gameId}";
        const currentUserId = "<%= currentUserId %>";
        const contextPath = "${pageContext.request.contextPath}";
    </script>
    
    <!-- Import Firebase SDK -->
    <script src="https://www.gstatic.com/firebasejs/9.6.1/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/9.6.1/firebase-firestore-compat.js"></script>
    
    <!-- Import file JS của bạn -->
    <script src="${pageContext.request.contextPath}/js/game.js"></script>
</body>
</html>