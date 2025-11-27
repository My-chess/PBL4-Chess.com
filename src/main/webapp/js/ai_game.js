document.addEventListener('DOMContentLoaded', function() {
   
    const boardElement = document.getElementById('board');
    const turnColorEl = document.getElementById('turn-color');
    const statusMessageEl = document.getElementById('status-message');
    const gameOverModal = document.getElementById('gameOverModal');
    const gameOverMessageEl = document.getElementById('gameOverMessage');
    const resignBtn = document.getElementById('resign-btn');
    const capturedForPlayerEl = document.getElementById('captured-for-player');
    const capturedForAiEl = document.getElementById('captured-for-ai');
 
    let selectedSquare = null; 
    let currentBoardState = {};  
    let isPlayerTurn = (playerColor === 'Red'); 
    let isGameActive = true;
    let aiProcessing = false; 
 
    const sounds = {
        move: new Audio(contextPath + '/sounds/move.mp3'),
        capture: new Audio(contextPath + '/sounds/capture.mp3'),
        gameOver: new Audio(contextPath + '/sounds/game-over.mp3')
    };
 
    const pieceToChar = { "KingBEAN": "將", "AdvisorBEAN": "士", "BishopBEAN": "象", "KnightBEAN": "馬", "RookBEAN": "車", "CannonBEAN": "炮", "PawnBEAN": "卒" };
    const redPieceToChar = { "KingBEAN": "帥", "AdvisorBEAN": "仕", "BishopBEAN": "相", "KnightBEAN": "傌", "RookBEAN": "俥", "CannonBEAN": "砲", "PawnBEAN": "兵" }; 
    const initialPieceCount = { "KingBEAN": 1, "AdvisorBEAN": 2, "BishopBEAN": 2, "KnightBEAN": 2, "RookBEAN": 2, "CannonBEAN": 2, "PawnBEAN": 5 };

    // --- 1. TÍNH KÍCH THƯỚC Ô CỜ (FIX LỆCH VỊ TRÍ) ---
    function getActualSquareSize() {
        const width = boardElement.offsetWidth;
        return width > 0 ? width / 8 : 65; 
    }

    // --- 2. CẬP NHẬT GIAO DIỆN (RESPONSIVE) ---
    function updateSquarePositions() {
        const squareSize = getActualSquareSize();
        const squares = boardElement.querySelectorAll('.square');
        squares.forEach(sq => {
            const x = parseInt(sq.dataset.x);
            const y = parseInt(sq.dataset.y);
            sq.style.left = (x * squareSize) + 'px';
            sq.style.top = (y * squareSize) + 'px';
            sq.style.width = squareSize + 'px';
            sq.style.height = squareSize + 'px';
        });
        const pieces = boardElement.querySelectorAll('.piece');
        pieces.forEach(p => {
            p.style.fontSize = (squareSize * 0.6) + 'px';
        });
    }

    // --- 3. VẼ BÀN CỜ ---
    function renderBoardFromState(boardState) {
        boardElement.innerHTML = '';  
        const squareSize = getActualSquareSize();
 
        // Vẽ ô cờ
        for (let y = 0; y < 10; y++) {
            for (let x = 0; x < 9; x++) {
                const square = document.createElement('div');
                square.className = 'square';
                square.dataset.x = x;
                square.dataset.y = y;
                square.style.position = 'absolute';
                square.style.left = (x * squareSize) + 'px';
                square.style.top = (y * squareSize) + 'px';
                square.style.width = squareSize + 'px';
                square.style.height = squareSize + 'px';
                boardElement.appendChild(square);
            }
        }
 
        // Vẽ quân cờ
        for (const key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                const [y, x] = key.split(',').map(Number);
                const [pieceType, color] = boardState[key].split('_');
                const targetSquare = boardElement.querySelector(`.square[data-x="${x}"][data-y="${y}"]`);
                if (targetSquare) {
                    const pieceElement = document.createElement('div');
                    pieceElement.className = `piece ${color.toLowerCase()}`;
                    pieceElement.textContent = (color === 'Red') ? redPieceToChar[pieceType] : pieceToChar[pieceType];
                    pieceElement.style.fontSize = (squareSize * 0.6) + 'px';
                    targetSquare.appendChild(pieceElement);
                }
            }
        }
        currentBoardState = boardState; 
    }

    // --- 4. CẬP NHẬT QUÂN BỊ ĂN ---
    function updateCapturedPieces(newBoardState) {
        try {
            const currentPieceCount = { Red: {}, Black: {} };
            for (const key in newBoardState) {
                if (newBoardState.hasOwnProperty(key)) {
                    const [, pieceType, color] = newBoardState[key].split('_');
                    currentPieceCount[color][pieceType] = (currentPieceCount[color][pieceType] || 0) + 1;
                }
            }
            const capturedFor = { Red: [], Black: [] };
            for (const pieceType in initialPieceCount) {
                if (initialPieceCount.hasOwnProperty(pieceType)) {
                    const initialCount = initialPieceCount[pieceType];
                    const redCount = currentPieceCount.Red[pieceType] || 0;
                    const blackCount = currentPieceCount.Black[pieceType] || 0;
                    for (let i = 0; i < initialCount - redCount; i++) capturedFor.Black.push(pieceType + '_Red');
                    for (let i = 0; i < initialCount - blackCount; i++) capturedFor.Red.push(pieceType + '_Black');
                }
            }
            const renderImg = (p) => `<img src="${contextPath}/images/${p}.png" alt="${p}" style="width:30px;height:30px;margin:2px;">`;
            capturedForPlayerEl.innerHTML = capturedFor[aiColor].map(renderImg).join('');
            capturedForAiEl.innerHTML = capturedFor[playerColor].map(renderImg).join('');
        } catch (e) { console.error("Lỗi cập nhật quân bị ăn:", e); }
    }

    // --- 5. TIỆN ÍCH HIGHLIGHT ---
    function clearHighlights() {
        document.querySelectorAll('.square.selected, .square.valid-move-dot').forEach(sq => {
            sq.classList.remove('selected', 'valid-move-dot');
        });
    }

    function showValidMoves(x, y) {
        clearHighlights();  
        const square = document.querySelector(`.square[data-x="${x}"][data-y="${y}"]`);
        if(square) square.classList.add('selected');

        const tempBoardArray = Array(10).fill(null).map(() => Array(9).fill(null));
        for (const key in currentBoardState) {
            if (currentBoardState.hasOwnProperty(key)) {
                const [r, c] = key.split(',').map(Number);
                const [type, color] = currentBoardState[key].split('_');
                tempBoardArray[r][c] = { type, color };
            }
        }

        if (typeof getValidMoves === 'function') {
            const validMoves = getValidMoves(tempBoardArray, parseInt(x), parseInt(y));
            validMoves.forEach(move => {
                const [endX, endY] = move;
                const sq = document.querySelector(`.square[data-x="${endX}"][data-y="${endY}"]`);
                if (sq) sq.classList.add('valid-move-dot');
            });
        }
    }

    function highlightLastMove(startX, startY, endX, endY) {
        document.querySelectorAll('.square.last-move').forEach(el => el.classList.remove('last-move'));
        
        // Dùng querySelector chính xác
        const startSq = document.querySelector(`.square[data-x="${startX}"][data-y="${startY}"]`);
        const endSq = document.querySelector(`.square[data-x="${endX}"][data-y="${endY}"]`);
        
        if (startSq) startSq.classList.add('last-move');
        if (endSq) endSq.classList.add('last-move');
    }

    // --- 6. XỬ LÝ GAME LOGIC CHÍNH (ĐÃ SỬA LỖI TREO) ---
    function handlePlayerMove(startX, startY, endX, endY) {
        if (!isGameActive || !isPlayerTurn || aiProcessing) return;
 
        aiProcessing = true;
        statusMessageEl.textContent = 'Máy đang suy nghĩ...';
        turnColorEl.textContent = 'Máy';
        turnColorEl.className = aiColor.toLowerCase();

        // Di chuyển trên UI ngay lập tức
        const startSq = document.querySelector(`.square[data-x="${startX}"][data-y="${startY}"]`);
        const endSq = document.querySelector(`.square[data-x="${endX}"][data-y="${endY}"]`);
        const piece = startSq ? startSq.querySelector('.piece') : null;
        if (piece && endSq) {
            const captured = endSq.querySelector('.piece');
            if (captured) captured.remove();
            endSq.appendChild(piece);
            sounds.move.play();
            highlightLastMove(startX, startY, endX, endY);
        }

        fetch(`${contextPath}/api/ai/move`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
            body: JSON.stringify({ startX, startY, endX, endY })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) { 
                // Delay 1 giây
                setTimeout(() => {
                    // DÙNG try...finally ĐỂ ĐẢM BẢO KHÔNG BỊ TREO
                    try {
                        // 1. Cập nhật bàn cờ
                        renderBoardFromState(data.newBoardState);
                        updateCapturedPieces(data.newBoardState);

                        // 2. Highlight nước máy
                        if (data.aiMove) {
                            sounds.move.play();
                            const sx = parseInt(data.aiMove.startX);
                            const sy = parseInt(data.aiMove.startY);
                            const ex = parseInt(data.aiMove.endX);
                            const ey = parseInt(data.aiMove.endY);
                            highlightLastMove(sx, sy, ex, ey);
                        }

                        // 3. Kiểm tra kết quả
                        if (data.gameState === 'CHECKMATE') {
                            endGame('CHECKMATE', false); 
                        } else if (data.gameState === 'DRAW') {
                            endGame('DRAW', false);
                        }
                    } catch (e) {
                        console.error("Lỗi khi xử lý phản hồi từ AI:", e);
                    } finally {
                        // 4. QUAN TRỌNG NHẤT: TRẢ LƯỢT CHO NGƯỜI CHƠI BẤT KỂ CÓ LỖI HAY KHÔNG
                        if (isGameActive && data.gameState === 'IN_PROGRESS') {
                            isPlayerTurn = true;
                            aiProcessing = false;
                            turnColorEl.textContent = 'Người chơi';
                            turnColorEl.className = playerColor.toLowerCase();
                            statusMessageEl.textContent = 'Đến lượt bạn!';
                        }
                    }
                }, 1000); 
            } else {
                alert(`Lỗi: ${data.message}`);
                location.reload(); 
            }
        })
        .catch(error => {
            console.error('Lỗi server:', error);
            statusMessageEl.textContent = 'Lỗi kết nối server.';
            aiProcessing = false; // Mở khóa nếu lỗi mạng
        });
    }

	function endGame(reason, playerWon) {
	        isGameActive = false;
	        aiProcessing = false;
	        sounds.gameOver.play();
	        gameOverModal.classList.add('show');
	        
	        let title = '';
	        let message = '';
	        let detail = '';

	        // Xác định tiêu đề thắng/thua
	        if (reason === 'RESIGN') {
	            title = 'THẤT BẠI';
	            message = '🏳️ Bạn đã đầu hàng.';
	        } else if (reason === 'DRAW') {
	            title = 'HÒA CỜ';
	            message = '🤝 Ván cờ kết thúc với tỉ số hòa.';
	            detail = '(Do lặp lại nước đi quá 3 lần)';
	        } else {
	            // Trường hợp CHECKMATE hoặc STALEMATE
	            if (playerWon) {
	                title = 'CHIẾN THẮNG!';
	                message = '🎉 Chúc mừng! Bạn đã đánh bại Máy.';
	            } else {
	                title = 'THẤT BẠI';
	                message = '💀 Bạn đã thua trước Máy.';
	            }

	            // Chi tiết lý do
	            if (reason === 'CHECKMATE') {
	                detail = '(Đối phương bị Chiếu bí)';
	            } else if (reason === 'STALEMATE') {
	                detail = '(Đối phương hết nước đi - Vây khốn)';
	            }
	        }
	        
	        // Cập nhật DOM (Giả sử trong HTML modal của bạn có thẻ h2 và p)
	        const modalTitle = gameOverModal.querySelector('h2');
	        const modalMsg = document.getElementById('gameOverMessage');
	        
	        if (modalTitle) modalTitle.textContent = title;
	        modalMsg.innerHTML = `${message}<br><span style="font-size: 0.9em; color: gray;">${detail}</span>`;
	        
	        statusMessageEl.textContent = 'Ván cờ đã kết thúc.';
	    }

    // --- 7. SỰ KIỆN CLICK BÀN CỜ ---
    boardElement.addEventListener('click', function(event) {
        if (!isGameActive || !isPlayerTurn || aiProcessing) return; 

        let clickedEl = event.target;
        if (clickedEl.classList.contains('piece')) clickedEl = clickedEl.parentNode;  
        if (!clickedEl.classList.contains('square')) return;  

        const clickedX = parseInt(clickedEl.dataset.x);
        const clickedY = parseInt(clickedEl.dataset.y);
        const pieceAtClicked = currentBoardState[`${clickedY},${clickedX}`]; 

        if (!selectedSquare) { 
            if (pieceAtClicked && pieceAtClicked.endsWith(playerColor)) {  
                selectedSquare = clickedEl;
                showValidMoves(clickedX, clickedY);
            }
        } else {
            const startX = parseInt(selectedSquare.dataset.x);
            const startY = parseInt(selectedSquare.dataset.y);

            if (selectedSquare === clickedEl) {
                clearHighlights();
                selectedSquare = null;
            } else if (clickedEl.classList.contains('valid-move-dot')) {
                clearHighlights();
                selectedSquare = null;
                handlePlayerMove(startX, startY, clickedX, clickedY);
            } else if (pieceAtClicked && pieceAtClicked.endsWith(playerColor)) {
                clearHighlights();
                selectedSquare = clickedEl;
                showValidMoves(clickedX, clickedY);
            } else {
                clearHighlights();
                selectedSquare = null;
            }
        }
    });
 
    resignBtn.addEventListener('click', function() {
        if (!isGameActive) return;
        if (confirm('Bạn muốn đầu hàng?')) {
            fetch(`${contextPath}/api/ai/resign`, { method: 'POST' })
            .then(r => r.json())
            .then(d => { if(d.success) endGame('RESIGN', false); });
        }
    });

    window.addEventListener('resize', updateSquarePositions);

    // --- 8. KHỞI TẠO GAME ---
    function initializeAiGame() { 
        if (!initialBoardState) {
            statusMessageEl.textContent = "Lỗi dữ liệu.";
            isGameActive = false;
            return;
        }

        renderBoardFromState(initialBoardState);
        updateCapturedPieces(initialBoardState);  
		
        if (aiColor === 'Red') {
            isPlayerTurn = false;
            aiProcessing = true;
            statusMessageEl.textContent = "Máy đi trước, đang suy nghĩ...";
            updateTurnIndicator();

            setTimeout(() => {
                fetch(`${contextPath}/api/ai/move`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ startX: -1, startY: -1, endX: -1, endY: -1 }) 
                })
                .then(r => r.json())
                .then(data => {
                    if (data.success) {
                        try {
                            renderBoardFromState(data.newBoardState);
                            updateCapturedPieces(data.newBoardState);
                            sounds.move.play();
                            if(data.aiMove) {
                                highlightLastMove(parseInt(data.aiMove.startX), parseInt(data.aiMove.startY), parseInt(data.aiMove.endX), parseInt(data.aiMove.endY));
                            }
                        } catch(e) { console.error(e); }
                        finally {
                            isPlayerTurn = true;
                            aiProcessing = false;
                            updateTurnIndicator();
                            statusMessageEl.textContent = 'Đến lượt bạn!';
                        }
                    } else {
                        statusMessageEl.textContent = `Lỗi: ${data.message}`;
                    }
                });
            }, 1000);
        } else {
            isPlayerTurn = true;
            updateTurnIndicator();
            statusMessageEl.textContent = 'Đến lượt bạn!';
        }
    }

    function updateTurnIndicator() {
        turnColorEl.textContent = isPlayerTurn ? 'Người chơi' : 'Máy';
        turnColorEl.className = isPlayerTurn ? playerColor.toLowerCase() : aiColor.toLowerCase();
    }
 
    initializeAiGame();
    window.addEventListener('beforeunload', () => sessionStorage.removeItem('ai_initial_board'));
});