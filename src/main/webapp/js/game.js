/**
 * File: game.js (Phiên bản ES5 - Tương thích Hoàn toàn)
 * Chịu trách nhiệm cho toàn bộ logic phía client của trang game.
 */

document.addEventListener('DOMContentLoaded', function() {
    
    // --- PHẦN 1: KHỞI TẠO VÀ KHAI BÁO ---

    // A. Lấy các thành phần UI
    var boardElement = document.getElementById('board');
    var turnColorEl = document.getElementById('turn-color');
    var statusMessageEl = document.getElementById('status-message');
    var gameOverModal = document.getElementById('gameOverModal');
    var gameOverMessageEl = document.getElementById('gameOverMessage');
    var offerDrawBtn = document.getElementById('offer-draw-btn');
    var resignBtn = document.getElementById('resign-btn');
    var gameActionsPanel = document.getElementById('game-actions-panel');
    var drawOfferPanel = document.getElementById('draw-offer-panel');
    var acceptDrawBtn = document.getElementById('accept-draw-btn');
    var declineDrawBtn = document.getElementById('decline-draw-btn');
    var capturedForRedEl = document.getElementById('captured-for-red');
    var capturedForBlackEl = document.getElementById('captured-for-black');

    // B. Biến quản lý trạng thái
    var selectedSquare = null;
    var websocket = null;
    var myColor = null;
    var isSpectator = false;
    var currentTurnFromServer = null;
    var timerInterval = null;
    var previousBoardState = null;

    // C. Khởi tạo âm thanh
    var sounds = {
        move: new Audio(contextPath + '/sounds/move.mp3'),
        capture: new Audio(contextPath + '/sounds/capture.mp3'),
        gameOver: new Audio(contextPath + '/sounds/game-over.mp3')
    };

    // D. Cấu hình Firebase
    var firebaseConfig = {
        apiKey: "AIzaSyBB2GREXzftCQkV41Rlwka1SPBulY1NVhI",
        authDomain: "pbl4-chess-fdcdb.firebaseapp.com",
        projectId: "pbl4-chess-fdcdb",
        storageBucket: "pbl4-chess-fdcdb.appspot.com",
        messagingSenderId: "1046253329967",
        appId: "1:1046253329967:web:d130010d7516699f0970da"
    };
    firebase.initializeApp(firebaseConfig);
    var db = firebase.firestore();
    console.log("✅ 1. Firebase Initialized Successfully.");

    console.log("✅ 2. Game ID: \"" + gameId + "\", User ID: \"" + currentUserId + "\"");
    if (!gameId) {
        statusMessageEl.textContent = "❌ Lỗi: Không tìm thấy ID ván cờ trong URL.";
        return;
    }

    // --- PHẦN 2: CÁC HÀM TIỆN ÍCH VÀ XỬ LÝ GIAO DIỆN (UI) ---

    /* ===== THÊM MỚI: Hàm Debounce (chống rung) cho sự kiện resize (ES5) ===== */
    function debounce(func, wait) {
        var timeout;
        return function() {
            var context = this, args = arguments;
            var later = function() {
                timeout = null;
                func.apply(context, args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /* ===== THÊM MỚI: Hàm cập nhật vị trí quân cờ khi resize ===== */
    function updateSquarePositions() {
        // Lấy kích thước ô vuông mới nhất từ CSS
        var squareSize = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--square-size'));
        var squares = boardElement.querySelectorAll('.square');
        
        // Lặp qua tất cả các ô .square và cập nhật lại style left/top
        for (var i = 0; i < squares.length; i++) {
            var sq = squares[i];
            var x = sq.dataset.x;
            var y = sq.dataset.y;
            sq.style.left = (x * squareSize) + 'px';
            sq.style.top = (y * squareSize) + 'px';
        }
    }

	function clearHighlights() {
	    var highlightedSquares = document.querySelectorAll('.square.selected, .square.valid-move-dot, .square.last-move');
	    for (var i = 0; i < highlightedSquares.length; i++) {
	        var sq = highlightedSquares[i];
	        sq.classList.remove('selected', 'valid-move-dot', 'last-move');
	    }
	}

	function showValidMoves(x, y) {
        // Giả định rằng hàm getValidMoves tồn tại (từ một tệp khác)
        // Nếu hàm này không tồn tại, đây sẽ là nơi logic thất bại.
        if (typeof getValidMoves === 'undefined') {
            console.error("Lỗi: Hàm 'getValidMoves' không được định nghĩa.");
            return;
        }

	    var boardArray = Array(10).fill(null).map(function() { return Array(9).fill(null); });
	    for (var key in previousBoardState) {
	        if (previousBoardState.hasOwnProperty(key)) {
	            var pos = key.split(',');
	            var row = parseInt(pos[0]);
	            var col = parseInt(pos[1]);
	            var pieceParts = previousBoardState[key].split('_');
	            boardArray[row][col] = { type: pieceParts[0], color: pieceParts[1] };
	        }
	    }

	    var moves = getValidMoves(boardArray, parseInt(x), parseInt(y));
	        
	    moves.forEach(function(move) {
	        var endX = move[0];
	        var endY = move[1];
	        var selector = '.square[data-x="' + endX + '"][data-y="' + endY + '"]';
	        var square = document.querySelector(selector);
	        if (square) {
	            square.classList.add('valid-move-dot');
	        }
	    });
	}
    
	// Các biến chữ Hán (đặt ở ngoài)
	var pieceToChar = { "KingBEAN": "將", "AdvisorBEAN": "士", "BishopBEAN": "象", "KnightBEAN": "馬", "RookBEAN": "車", "CannonBEAN": "炮", "PawnBEAN": "卒" };
	var redPieceToChar = { "KingBEAN": "帥", "AdvisorBEAN": "仕", "BishopBEAN": "相", "KnightBEAN": "傌", "RookBEAN": "俥", "CannonBEAN": "砲", "PawnBEAN": "兵" };

	function renderBoardFromState(boardState) {
	    boardElement.innerHTML = ''; // Xóa bàn cũ

	    var squareSize = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--square-size'));

	    // 🔹 Vẽ 90 ô vuông (9 cột × 10 hàng)
	    for (var y = 0; y < 10; y++) {
	        for (var x = 0; x < 9; x++) {
	            var square = document.createElement('div');
	            square.className = 'square';

	            // 👉 Nếu người chơi là ĐEN, đảo toạ độ để Đen ở dưới
	            var displayX = (myColor === 'Black') ? 8 - x : x;
	            var displayY = (myColor === 'Black') ? 9 - y : y;

	            square.dataset.x = x;
	            square.dataset.y = y;
	            square.style.left = (displayX * squareSize) + 'px';
	            square.style.top = (displayY * squareSize) + 'px';

	            boardElement.appendChild(square);
	        }
	    }

	    // 🔹 Vẽ quân cờ
	    for (var key in boardState) {
	        if (!boardState.hasOwnProperty(key)) continue;

	        var pos = key.split(',');
	        var y = parseInt(pos[0]);
	        var x = parseInt(pos[1]);
	        var parts = boardState[key].split('_');
	        var pieceType = parts[0];
	        var color = parts[1];

	        // 👉 Tọa độ hiển thị cũng phải đảo tương tự
	        var displayX = (myColor === 'Black') ? 8 - x : x;
	        var displayY = (myColor === 'Black') ? 9 - y : y;

	        var squareSelector = '.square[data-x="' + x + '"][data-y="' + y + '"]';
	        var targetSquare = boardElement.querySelector(squareSelector);
	        if (!targetSquare) continue;

	        var pieceEl = document.createElement('div');
	        pieceEl.className = 'piece ' + color.toLowerCase();
	        pieceEl.textContent = (color === 'Red') ? redPieceToChar[pieceType] : pieceToChar[pieceType];

	        // 🔹 Nếu bạn là ĐEN, lật chữ quân cờ cho đúng hướng
	        if (myColor === 'Black') {
	            pieceEl.style.transform = 'rotate(180deg)';
	        }

	        targetSquare.appendChild(pieceEl);
	    }

	    // 🔹 Nếu bạn là ĐEN, lật toàn bộ bàn
	    if (myColor === 'Black') {
	        boardElement.style.transform = 'rotate(180deg)';
	    } else {
	        boardElement.style.transform = 'rotate(0deg)';
	    }
	}



	function processBoardChanges(newBoardState) {
        if (!previousBoardState) {
            previousBoardState = newBoardState;
            updateCapturedPieces(newBoardState);
            return;
        }

        var startKey = null, endKey = null;
        var wasCapture = false;

        var oldKeys = Object.keys(previousBoardState);
        var newKeys = Object.keys(newBoardState);
	        
        if (oldKeys.length > newKeys.length) wasCapture = true;
        else if (oldKeys.length === newKeys.length) {
             for (var i = 0; i < newKeys.length; i++) {
                var key = newKeys[i];
                if (previousBoardState.hasOwnProperty(key) && newBoardState[key] !== previousBoardState[key]) {
                    wasCapture = true;
                    break;
                }
            }
        }

        /* ===== SỬA LỖI (ES5): Thay thế 'new Set()' bằng object lookup ===== */
        var oldKeySet = {};
        for (var i = 0; i < oldKeys.length; i++) { oldKeySet[oldKeys[i]] = true; }
        
        var newKeySet = {};
        for (var i = 0; i < newKeys.length; i++) { newKeySet[newKeys[i]] = true; }

        for (var i = 0; i < oldKeys.length; i++) { 
            if (!newKeySet[oldKeys[i]] || (wasCapture && newBoardState[oldKeys[i]] !== previousBoardState[oldKeys[i]])) { 
                startKey = oldKeys[i]; 
                break; 
            } 
        }
        for (var i = 0; i < newKeys.length; i++) { 
            if (!oldKeySet[newKeys[i]] || (wasCapture && newBoardState[newKeys[i]] !== previousBoardState[newKeys[i]])) { 
                endKey = newKeys[i]; 
                break; 
            } 
        }
        /* ===== KẾT THÚC SỬA LỖI ES5 ===== */
	        
        clearHighlights();
	        
        if (startKey && endKey) {
            var startCoords = startKey.split(',');
            var endCoords = endKey.split(',');
            var startY = startCoords[0], startX = startCoords[1];
            var endY = endCoords[0], endX = endCoords[1];
            var startSelector = '.square[data-x="' + startX + '"][data-y="' + startY + '"]';
            var endSelector = '.square[data-x="' + endX + '"][data-y="' + endY + '"]';
            if (document.querySelector(startSelector)) document.querySelector(startSelector).classList.add('last-move');
            if (document.querySelector(endSelector)) document.querySelector(endSelector).classList.add('last-move');
        }
        updateCapturedPieces(newBoardState);
        if (wasCapture) sounds.capture.play();
        else if (startKey) sounds.move.play();
        previousBoardState = newBoardState;
	}

	function updateCapturedPieces(boardState) {
        var initialPieceCount = { "KingBEAN":1, "AdvisorBEAN":2, "BishopBEAN":2, "KnightBEAN":2, "RookBEAN":2, "CannonBEAN":2, "PawnBEAN":5 };
        var currentPieceCount = { Red: {}, Black: {} };
        for (var key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                var parts = boardState[key].split('_');
                var pieceType = parts[0];
                var color = parts[1];
                currentPieceCount[color][pieceType] = (currentPieceCount[color][pieceType] || 0) + 1;
            }
        }
        var capturedFor = { Red: [], Black: [] };
        for (var pieceType in initialPieceCount) {
            if (initialPieceCount.hasOwnProperty(pieceType)) {
                var initialCount = initialPieceCount[pieceType];
                var redCount = currentPieceCount.Red[pieceType] || 0;
                var blackCount = currentPieceCount.Black[pieceType] || 0;
                for (var i = 0; i < initialCount - redCount; i++) capturedFor.Black.push(pieceType + '_Red');
                for (var i = 0; i < initialCount - blackCount; i++) capturedFor.Red.push(pieceType + '_Black');
            }
        }
        capturedForRedEl.innerHTML = capturedFor.Red.map(function(p) { return '<img src="' + contextPath + '/images/' + p + '.png" alt="' + p + '">'; }).join('');
        capturedForBlackEl.innerHTML = capturedFor.Black.map(function(p) { return '<img src="' + contextPath + '/images/' + p + '.png" alt="' + p + '">'; }).join('');
	}

    function formatTime(ms) {
        if (ms <= 0) return "00:00";
        var totalSeconds = Math.floor(ms / 1000);
        var minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
        var seconds = (totalSeconds % 60).toString().padStart(2, '0');
        return minutes + ':' + seconds;
    }
    
    function updateAndStartTimer(matchData) {
        clearInterval(timerInterval);
        if (!matchData.lastMoveTimestamp || matchData.status !== 'IN_PROGRESS') return;
        var serverNow = firebase.firestore.Timestamp.now().toMillis();
        var lastMoveTime = matchData.lastMoveTimestamp.toMillis();
        var timeElapsed = serverNow - lastMoveTime;
        var p1Time = matchData.player1TimeLeftMs;
        var p2Time = matchData.player2TimeLeftMs;
        if (matchData.currentTurn === 'Red') p1Time -= timeElapsed;
        else p2Time -= timeElapsed;

        timerInterval = setInterval(function() {
            var p1Display = document.getElementById('timer-red');
            var p2Display = document.getElementById('timer-black');
            p1Display.textContent = formatTime(p1Time);
            p2Display.textContent = formatTime(p2Time);

            if (matchData.currentTurn === 'Red') {
                p1Time -= 1000;
                p1Display.classList.add('active');
                p2Display.classList.remove('active');
                if (p1Time < 0 && myColor === 'Red' && websocket && websocket.readyState === WebSocket.OPEN) {
                    clearInterval(timerInterval);
                    websocket.send(JSON.stringify({ type: 'timeout' }));
                }
            } else {
                p2Time -= 1000;
                p2Display.classList.add('active');
                p1Display.classList.remove('active');
                if (p2Time < 0 && myColor === 'Black' && websocket && websocket.readyState === WebSocket.OPEN) {
                    clearInterval(timerInterval);
                    websocket.send(JSON.stringify({ type: 'timeout' }));
                }
            }
        }, 1000);
    }

    // --- PHẦN 3: LOGIC CHÍNH VÀ LUỒNG SỰ KIỆN ---

    function initializeGame() {
        listenToMatchUpdates();
        addEventListeners();
        connectWebSocket();
    }

    function connectWebSocket() {
        var isSecure = window.location.protocol === 'https:';
        var protocol = isSecure ? 'wss://' : 'ws://';
        var wsUrl = protocol + window.location.host + contextPath + '/game/' + gameId + '/' + currentUserId;
        console.log("✅ 3. Attempting to connect to WebSocket at: " + wsUrl);
        websocket = new WebSocket(wsUrl);

        websocket.onopen = function() { console.log("✅ WebSocket Connection Established!"); };
        websocket.onclose = function(event) {
            console.warn("WebSocket Connection Closed.", event);
            statusMessageEl.textContent = "Mất kết nối server.";
            clearInterval(timerInterval);
        };
        websocket.onerror = function(err) { console.error("❌ WebSocket Error:", err); };
        
        websocket.onmessage = function(event) {
            console.log(">>>>>> WebSocket message received!");
            var data = JSON.parse(event.data);
            console.log("Parsed data:", data);
            
            switch (data.type) {
                case 'ERROR':
                    statusMessageEl.textContent = 'Lỗi: ' + data.message;
                    if (selectedSquare) {
                        // Thêm class để rung lắc (cần CSS)
                        // selectedSquare.classList.add('error-shake'); 
                        setTimeout(function() {
                            clearHighlights();
                            selectedSquare = null;
                            statusMessageEl.textContent = (currentTurnFromServer === myColor) ? "Đến lượt bạn!" : "Đang chờ đối thủ...";
                        }, 600);
                    }
                    break;
                case 'GAME_OVER':
                    console.log(">>>>>> GAME_OVER case triggered!");
                    clearInterval(timerInterval);
                    statusMessageEl.textContent = "Ván cờ đã kết thúc.";
                    sounds.gameOver.play();
                    var finalMessage = '';
                    if (data.winnerColor) {
                        var winnerText = data.winnerColor === 'Red' ? "Phe Đỏ" : "Phe Đen";
                        var reasonText = '';
                        switch(data.reason) {
                            case 'CHECKMATE': reasonText = 'do chiếu bí'; break;
                            case 'TIMEOUT': reasonText = 'do đối thủ hết giờ'; break;
                            case 'RESIGN': reasonText = 'do đối thủ đầu hàng'; break;
                            case 'DISCONNECT': reasonText = 'do đối thủ mất kết nối'; break;
                            case 'AFK_TIMEOUT': reasonText = 'do đối thủ không đi cờ'; break;
                            default: reasonText = '';
                        }
                        finalMessage = winnerText + ' đã giành chiến thắng ' + reasonText + '!';
                    } else {
                        var reasonText = '';
                        switch(data.reason) {
                            case 'DRAW_AGREEMENT': reasonText = 'Hai bên đồng ý hòa cờ.'; break;
                            case 'DRAW_REPETITION': reasonText = 'Hòa do lặp lại nước đi 3 lần.'; break;
                            default: reasonText = 'Ván cờ kết thúc với tỉ số hòa.'; break;
                        }
                        finalMessage = reasonText;
                    }
                    gameOverMessageEl.textContent = finalMessage;
                    gameOverModal.style.display = 'flex';
                    gameActionsPanel.style.display = 'none';
                    drawOfferPanel.style.display = 'none';
                    break;
                case 'DRAW_OFFER_RECEIVED':
                    statusMessageEl.textContent = "Đối thủ muốn hòa cờ!";
                    gameActionsPanel.style.display = 'none';
                    drawOfferPanel.style.display = 'block';
                    break;
                case 'DRAW_OFFER_DECLINED':
                    statusMessageEl.textContent = "Đối thủ đã từ chối hòa cờ.";
                    offerDrawBtn.disabled = false;
                    offerDrawBtn.textContent = 'Cầu hòa';
                    setTimeout(function() { 
                         statusMessageEl.textContent = (currentTurnFromServer === myColor) ? "Đến lượt bạn!" : "Đang chờ đối thủ...";
                    }, 3000);
                    break;
            }
        };
    }

    function addEventListeners() {
        offerDrawBtn.addEventListener('click', function() {
            if (confirm('Bạn có chắc muốn gửi lời mời hòa cờ?')) {
                websocket.send(JSON.stringify({ type: 'offer_draw' }));
                offerDrawBtn.disabled = true;
                offerDrawBtn.textContent = 'Đã gửi lời mời...';
            }
        });
        resignBtn.addEventListener('click', function() {
            if (confirm('Bạn có chắc chắn muốn đầu hàng ván cờ này không?')) {
                websocket.send(JSON.stringify({ type: 'resign' }));
            }
        });
        acceptDrawBtn.addEventListener('click', function() {
            websocket.send(JSON.stringify({ type: 'accept_draw' }));
            drawOfferPanel.style.display = 'none';
            gameActionsPanel.style.display = 'block';
        });
        declineDrawBtn.addEventListener('click', function() {
            websocket.send(JSON.stringify({ type: 'decline_draw' }));
            drawOfferPanel.style.display = 'none';
            gameActionsPanel.style.display = 'block';
        });
        
		boardElement.addEventListener('click', function(event) {
	        if (isSpectator || currentTurnFromServer !== myColor) return;

	        // SỬA LỖI: Cần xác định mục tiêu click là .piece hay .square
	        var clickedEl = event.target;
	        var clickedSquare;

	        if (clickedEl.classList.contains('piece')) {
	            clickedSquare = clickedEl.parentNode; // Nếu click vào quân cờ, lấy ô .square cha
	        } else if (clickedEl.classList.contains('square')) {
	            clickedSquare = clickedEl; // Nếu click vào ô trống
	        } else {
	            return; // Click ra ngoài, không làm gì cả
	        }
		        
	        if (!clickedSquare) return;

	        var clickedPieceEl = clickedSquare.querySelector('.piece');
	        var clickedX = clickedSquare.dataset.x;
	        var clickedY = clickedSquare.dataset.y;

	        if (selectedSquare) { // Đã chọn một quân cờ trước đó
	            // Trường hợp 1: Click vào một nước đi hợp lệ
	            if (clickedSquare.classList.contains('valid-move-dot')) {
	                var startX = selectedSquare.dataset.x;
	                var startY = selectedSquare.dataset.y;
	                var message = { type: "move", data: { "startX": parseFloat(startX), "startY": parseFloat(startY), "endX": parseFloat(clickedX), "endY": parseFloat(clickedY) }};
	                websocket.send(JSON.stringify(message));
	                statusMessageEl.textContent = "Đã gửi nước đi, chờ xác nhận...";
	                clearHighlights();
	                selectedSquare = null;
	            } 
	            // Trường hợp 2: Click vào một quân cờ khác cùng màu -> Đổi lựa chọn
	            else if (clickedPieceEl && clickedPieceEl.className.indexOf(myColor.toLowerCase()) > -1) {
	                clearHighlights();
	                selectedSquare = clickedSquare;
	                selectedSquare.classList.add('selected'); // Dùng class 'selected' cho ô .square
	                showValidMoves(clickedX, clickedY);
	            }
	            // Trường hợp 3: Click vào một ô không hợp lệ -> Hủy chọn
	            else {
	                clearHighlights();
	                selectedSquare = null;
	            }
	        } else if (clickedPieceEl) { // Chưa chọn quân cờ nào và click vào một quân cờ
	            // Chỉ cho phép chọn quân cờ của mình
	            if (clickedPieceEl.className.indexOf(myColor.toLowerCase()) > -1) {
	                selectedSquare = clickedSquare;
	                selectedSquare.classList.add('selected'); // Dùng class 'selected' cho ô .square
	                showValidMoves(clickedX, clickedY);
	            }
	        }
	    });

        /* ===== THÊM MỚI: Lắng nghe sự kiện resize để sửa lỗi hiển thị ===== */
        window.addEventListener('resize', debounce(updateSquarePositions, 100));
    }

	function listenToMatchUpdates() {
	    var matchDocRef = db.collection('matches').doc(gameId);
	    matchDocRef.onSnapshot(function (doc) {
	        console.log("✅ 4. Match listener triggered. UI will be updated.");
	        if (!doc.exists) {
	            statusMessageEl.textContent = "Lỗi: Ván cờ này không còn tồn tại.";
	            return;
	        }

	        var matchData = doc.data();

	        // ⚠️ LỖI NHỎ CỦA BẠN: bạn dùng "data" thay vì "matchData"
	        // ✅ Sửa lại đúng:
	        let currentColor = null;
	        if (matchData.player1 && matchData.player1.uid === currentUserId) {
	            currentColor = "red";
	        } else if (matchData.player2 && matchData.player2.uid === currentUserId) {
	            currentColor = "black";
	        }

	        // 🎯 Xoay bàn cờ theo màu của người chơi
	        const boardEl = document.getElementById("board");
	        if (currentColor === "black") {
	            boardEl.style.transform = "rotate(180deg)";
	            boardEl.querySelectorAll(".piece").forEach(piece => {
	                piece.style.transform = "rotate(180deg)";
	            });
	        } else {
	            boardEl.style.transform = "rotate(0deg)";
	            boardEl.querySelectorAll(".piece").forEach(piece => {
	                piece.style.transform = "rotate(0deg)";
	            });
	        }

	        // --- Cập nhật trạng thái bàn cờ ---
	        currentTurnFromServer = matchData.currentTurn;
	        if (matchData.boardState) {
	            renderBoardFromState(matchData.boardState);
	            processBoardChanges(matchData.boardState);
	        }

	        // --- Cập nhật tên người chơi và ELO ---
	        var p1 = matchData.player1;
	        var p2 = matchData.player2;
	        document.getElementById('player-red-name').textContent = p1 ? p1.displayName : "Chờ người chơi...";
	        document.getElementById('player-black-name').textContent = p2 ? p2.displayName : "Chờ người chơi...";
	        document.getElementById('player-red-elo').textContent = (p1 && p1.elo) ? 'ELO: ' + p1.elo : "";
	        document.getElementById('player-black-elo').textContent = (p2 && p2.elo) ? 'ELO: ' + p2.elo : "";

	        // --- Xác định bạn là ai ---
	        if (p1 && p1.uid === currentUserId) {
	            myColor = "Red";
	            isSpectator = false;
	        } else if (p2 && p2.uid === currentUserId) {
	            myColor = "Black";
	            isSpectator = false;
	        } else {
	            myColor = null;
	            isSpectator = true;
	        }

	        // --- Cập nhật thông báo trạng thái ---
	        if (matchData.status === 'IN_PROGRESS') {
	            turnColorEl.textContent = matchData.currentTurn;
	            turnColorEl.className = matchData.currentTurn.toLowerCase();
	            if (!isSpectator) {
	                statusMessageEl.textContent =
	                    (matchData.currentTurn === myColor)
	                        ? "Đến lượt bạn!"
	                        : "Đang chờ đối thủ...";
	            } else {
	                statusMessageEl.textContent = 'Đang xem: Lượt của quân ' + matchData.currentTurn;
	            }
	        } else if (matchData.status === 'WAITING') {
	            statusMessageEl.textContent = 'Đang chờ người chơi khác... Mã phòng: ' + gameId;
	        }

	        updateAndStartTimer(matchData);
	    },
	    function (error) {
	        console.error("❌ Firestore Error on Match listener: ", error);
	    });
	}


    // --- PHẦN 4: KHỞI CHẠY ---
    initializeGame();

});