<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Xem lại Ván Cờ #${matchId}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/game.css">
    <style>
        .replay-controls { text-align: center; margin-top: 15px; }
        .replay-controls button { font-size: 16px; padding: 8px 15px; margin: 0 5px; }
        #move-info { margin-top: 10px; font-style: italic; }
    </style>
</head>
<body>
    <div class="game-container">
        <div class="main-content">
            <div class="player-info-panel top">...</div>
            <div class="board-frame"><div id="board"></div></div>
            <div class="player-info-panel bottom">...</div>
        </div>

        <div class="sidebar">
            <h2>Xem lại ván cờ</h2>
            <h3>#${matchId}</h3>
            <div class="replay-controls">
                <button id="btn-start">|&lt;</button>
                <button id="btn-prev">&lt;</button>
                <button id="btn-next">&gt;</button>
                <button id="btn-end">&gt;|</button>
            </div>
            <div id="move-info">
                Nước đi: <span id="move-counter">0</span> / <span id="total-moves">0</span>
            </div>
            <p id="move-description"></p>
        </div>
    </div>
    
    <script>
        const matchId = "${matchId}";
        const contextPath = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/js/replay.js"></script>
</body>
</html>