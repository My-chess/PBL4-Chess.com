<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Xem lại Ván Cờ #${matchId}</title>
    <%-- Sử dụng chung file game.css để đồng bộ giao diện --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/game.css">
    
    <%-- CSS bổ sung cho các nút điều khiển replay --%>
    <style>
        /* Giao diện các nút điều khiển trong sidebar */
        .replay-controls {
            text-align: center;
            margin: 20px 0;
            display: flex;
            justify-content: center;
            gap: 10px;
        }
        .replay-controls button {
            font-size: 20px;
            padding: 10px 20px;
            border-radius: 8px;
            border: 1px solid var(--border-light);
            background: var(--bg-card);
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .replay-controls button:hover:not(:disabled) {
            background: var(--primary-blue);
            color: white;
            border-color: var(--primary-blue);
        }
        .replay-controls button:disabled {
            color: #aaa;
            cursor: not-allowed;
        }

        /* Thông tin nước đi */
        #move-info {
            text-align: center;
            margin-bottom: 15px;
            font-size: 1.1em;
            color: var(--text-light);
        }
        #move-description {
            background: rgba(74, 144, 226, 0.05);
            padding: 15px;
            border-radius: 8px;
            min-height: 50px;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="game-container">
        <div class="main-content">
            <%-- THAY ĐỔI: Cập nhật cấu trúc panel cho giống game.jsp --%>
            <div class="player-info-panel top">
                <div class="player-details">
                    <span class="player-name" id="player-black-name">Người chơi Đen</span>
                    <span class="player-elo" id="player-black-elo"></span>
                </div>
            </div>

            <div class="board-frame">
                <div id="board">
                    <%-- JavaScript sẽ vẽ bàn cờ và quân cờ ở đây --%>
                </div>
            </div>
            
            <%-- THAY ĐỔI: Cập nhật cấu trúc panel cho giống game.jsp --%>
            <div class="player-info-panel bottom">
                 <div class="player-details">
                    <span class="player-name" id="player-red-name">Người chơi Đỏ</span>
                    <span class="player-elo" id="player-red-elo"></span>
                </div>
            </div>
        </div>

        <div class="sidebar">
            <h2>Xem lại ván cờ</h2>
            <h3 style="color: var(--text-light); text-align: center; margin-top: -10px;">#${matchId}</h3>
            
            <div class="replay-controls">
                <button id="btn-start" title="Về đầu">|&lt;</button>
                <button id="btn-prev" title="Lùi lại">&lt;</button>
                <button id="btn-next" title="Tiến tới">&gt;</button>
                <button id="btn-end" title="Đến cuối">&gt;|</button>
            </div>

            <div id="move-info">
                Nước đi: <span id="move-counter">0</span> / <span id="total-moves">0</span>
            </div>

            <p id="move-description">Đang tải dữ liệu ván cờ...</p>
        </div>
    </div>
    
    <script>
        const matchId = "${matchId}";
        const contextPath = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/js/replay.js"></script>
</body>
</html>