<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="Model.BEAN.UserBEAN" %>
<%
// Lấy thông tin user từ session
UserBEAN loggedInUser = (UserBEAN) session.getAttribute("loggedInUser");
String currentUserId = (loggedInUser != null) ? loggedInUser.getUid() : "spectator";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ván Cờ #${param.gameId}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/game.css">
</head>
<body>
    <div class="game-container">
        <!-- Phần nội dung chính -->
        <div class="main-content">
            <!-- Thông tin người chơi Đen (phía trên) -->
            <div class="player-info-panel top">
                <div class="captured-pieces" id="captured-for-black"></div>
                <div class="player-details">
                    <span class="player-name" id="player-black-name">Chờ người chơi...</span>
                    <span class="player-elo" id="player-black-elo"></span>
                </div>
                <div class="timer" id="timer-black">10:00</div>
            </div>

            <!-- Bàn cờ -->
            <div class="board-frame">
                <div id="board">
                    <!-- JavaScript sẽ tạo các phần tử bàn cờ và quân cờ ở đây -->
                </div>
            </div>

            <!-- Thông tin người chơi Đỏ (phía dưới) -->
            <div class="player-info-panel bottom">
                <div class="captured-pieces" id="captured-for-red"></div>
                <div class="player-details">
                    <span class="player-name" id="player-red-name">Chờ người chơi...</span>
                    <span class="player-elo" id="player-red-elo"></span>
                </div>
                <div class="timer active" id="timer-red">10:00</div>
            </div>
        </div>

        <!-- Sidebar -->
        <div class="sidebar">
            <h2>Ván cờ #${param.gameId}</h2>
            
            <div id="turn-indicator">
                Lượt đi: <span id="turn-color" class="red">...</span>
            </div>
            
            <div id="status-message">Đang tải ván cờ...</div>
            
            <div class="move-history">
                <h3>Lịch sử nước đi</h3>
                <ul id="move-list">
                    <!-- Nước đi sẽ được thêm vào đây -->
                </ul>
            </div>
            
            <div class="game-actions" id="game-actions-panel">
                <button id="offer-draw-btn">Cầu hòa</button>
                <button id="resign-btn">Đầu hàng</button>
            </div>

            <div class="draw-offer-response" id="draw-offer-panel" style="display: none;">
                <p>Đối thủ đã cầu hòa. Bạn có đồng ý không?</p>
                <button id="accept-draw-btn">Chấp nhận</button>
                <button id="decline-draw-btn">Từ chối</button>
            </div>
        </div>
    </div>

    <!-- Modal kết thúc game -->
    <div id="gameOverModal" class="modal-overlay">
        <div class="modal-content">
            <h2>Kết quả ván cờ</h2>
            <p id="gameOverMessage"></p>
            <a href="${pageContext.request.contextPath}/index.jsp" class="modal-button">Về trang chủ</a>
        </div>
    </div>

    <!-- Truyền biến từ JSP sang JavaScript -->
    <script>
        const gameId = "${param.gameId}";
        const currentUserId = "<%= currentUserId %>";
        const contextPath = "${pageContext.request.contextPath}";
    </script>

    <!-- Import Firebase SDK -->
    <script src="https://www.gstatic.com/firebasejs/9.6.1/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/9.6.1/firebase-firestore-compat.js"></script>

    <!-- Import file JS -->
    <script src="${pageContext.request.contextPath}/js/chess-logic.js"></script>
    <script src="${pageContext.request.contextPath}/js/game.js"></script>
</body>
</html>