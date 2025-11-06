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
 
    const sounds = {
        move: new Audio(contextPath + '/sounds/move.mp3'),
        capture: new Audio(contextPath + '/sounds/capture.mp3'),
        gameOver: new Audio(contextPath + '/sounds/game-over.mp3')
    };
 
    const pieceToChar = {
        "KingBEAN": "將", "AdvisorBEAN": "士", "BishopBEAN": "象",
        "KnightBEAN": "馬", "RookBEAN": "車", "CannonBEAN": "炮", "PawnBEAN": "卒"
    };
    const redPieceToChar = {
        "KingBEAN": "帥", "AdvisorBEAN": "仕", "BishopBEAN": "相",
        "KnightBEAN": "傌", "RookBEAN": "俥", "CannonBEAN": "砲", "PawnBEAN": "兵"
    }; 
    const initialPieceCount = {
        "KingBEAN": 1, "AdvisorBEAN": 2, "BishopBEAN": 2,
        "KnightBEAN": 2, "RookBEAN": 2, "CannonBEAN": 2, "PawnBEAN": 5
    };

 
    function updateSquarePositions() {
        const squareSize = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--square-size'));
        const squares = boardElement.querySelectorAll('.square');
        squares.forEach(sq => {
            const x = sq.dataset.x;
            const y = sq.dataset.y;
            sq.style.left = (x * squareSize) + 'px';
            sq.style.top = (y * squareSize) + 'px';
        });
    }

 
    function clearHighlights() {
        document.querySelectorAll('.square.selected, .square.valid-move-dot, .square.last-move')
            .forEach(sq => {
                sq.classList.remove('selected', 'valid-move-dot', 'last-move');
            });
    }

 
    function showValidMoves(x, y) {
        clearHighlights();  
        const piece = currentBoardState[`${y},${x}`];
        if (!piece) return;

 
        const tempBoardArray = create2DArrayFromBoardState(currentBoardState);
        const validMoves = getValidMoves(tempBoardArray, parseInt(x), parseInt(y));

        validMoves.forEach(move => {
            const [endX, endY] = move;
            const selector = `.square[data-x="${endX}"][data-y="${endY}"]`;
            const square = document.querySelector(selector);
            if (square) {
                square.classList.add('valid-move-dot');
            }
        });
    }
 
    function renderBoardFromState(boardState) {
        boardElement.innerHTML = '';  
        const squareSize = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--square-size')) || 65;
 
        for (let y = 0; y < 10; y++) {
            for (let x = 0; x < 9; x++) {
                const square = document.createElement('div');
                square.className = 'square';
                square.dataset.x = x;
                square.dataset.y = y;
                square.style.left = (x * squareSize) + 'px';
                square.style.top = (y * squareSize) + 'px';
                boardElement.appendChild(square);
            }
        }
 
        for (const key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                const [y, x] = key.split(',').map(Number);
                const pieceInfo = boardState[key];
                const [pieceType, color] = pieceInfo.split('_');

                const selector = `.square[data-x="${x}"][data-y="${y}"]`;
                const targetSquare = boardElement.querySelector(selector);

                if (targetSquare) {
                    const pieceElement = document.createElement('div');
                    pieceElement.className = `piece ${color.toLowerCase()}`;
                    pieceElement.textContent = (color === 'Red') ? redPieceToChar[pieceType] : pieceToChar[pieceType];
                    targetSquare.appendChild(pieceElement);
                }
            }
        }
        currentBoardState = boardState; 
    }
 
    function updateCapturedPieces(newBoardState) {
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

        capturedForPlayerEl.innerHTML = capturedFor[aiColor].map(p =>
            `<img src="${contextPath}/images/${p}.png" alt="${p}">`
        ).join('');
        capturedForAiEl.innerHTML = capturedFor[playerColor].map(p =>
            `<img src="${contextPath}/images/${p}.png" alt="${p}">`
        ).join('');
    }

 
    function create2DArrayFromBoardState(boardState) {
        const boardArray = Array(10).fill(null).map(() => Array(9).fill(null));
        for (const key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                const [y, x] = key.split(',').map(Number);
                const [type, color] = boardState[key].split('_');
                boardArray[y][x] = { type: type, color: color };
            }
        }
        return boardArray;
    }

 
    function updateTurnIndicator() {
        turnColorEl.textContent = isPlayerTurn ? 'Người chơi' : 'Máy';
        turnColorEl.className = isPlayerTurn ? playerColor.toLowerCase() : aiColor.toLowerCase();
        statusMessageEl.textContent = isPlayerTurn ? 'Đến lượt bạn!' : 'Máy đang suy nghĩ...';
    }

 
    function handlePlayerMove(startX, startY, endX, endY) {
        if (!isGameActive || !isPlayerTurn) return;
 
        fetch(`${contextPath}/api/ai/move`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'  
            },
            body: JSON.stringify({ startX, startY, endX, endY })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) { 
                renderBoardFromState(data.newBoardState); 
                updateCapturedPieces(data.newBoardState);
                sounds.move.play();
                highlightLastMove(startX, startY, endX, endY);

 
                if (data.gameState !== 'IN_PROGRESS') {
                    endGame(data.gameState);
                    return;
                }

                isPlayerTurn = false;
                updateTurnIndicator();
 
                setTimeout(() => {
                    handleAiMove(data.aiMove);  
                    renderBoardFromState(data.newBoardState);
                    updateCapturedPieces(data.newBoardState);
                    sounds.move.play();
                    highlightLastMove(data.aiMove.startX, data.aiMove.startY, data.aiMove.endX, data.aiMove.endY);

                   
                    if (data.gameState !== 'IN_PROGRESS') {
                        endGame(data.gameState);
                        return;
                    }

                    isPlayerTurn = true;
                    updateTurnIndicator();
                }, 1000);  
            } else {
                statusMessageEl.textContent = `Lỗi: ${data.message || 'Nước đi không hợp lệ.'}`;
                clearHighlights();
                selectedSquare = null;
            }
        })
        .catch(error => {
            console.error('Lỗi khi gửi nước đi:', error);
            statusMessageEl.textContent = 'Lỗi server: Không thể gửi nước đi.';
            clearHighlights();
            selectedSquare = null;
        });
    }

    // Hàm xử lý nước đi của máy (chỉ hiển thị)
    function handleAiMove(aiMove) {
    
    }

 
    function endGame(reason) {
        isGameActive = false;
        sounds.gameOver.play();
        gameOverModal.style.display = 'flex';
        let message = '';
        if (reason === 'CHECKMATE') {
            message = isPlayerTurn ? 'Bạn thắng! Chiếu bí.' : 'Máy thắng! Chiếu bí.';
        } else if (reason === 'RESIGN') {
             message = isPlayerTurn ? 'Bạn đã đầu hàng.' : 'Máy đã đầu hàng.'; 
        } else if (reason === 'DRAW') {
            message = 'Hòa cờ.';
        } else {
            message = 'Ván cờ kết thúc.';
        }
        gameOverMessageEl.textContent = message;
        statusMessageEl.textContent = 'Ván cờ đã kết thúc.';
    }

 
    function highlightLastMove(startX, startY, endX, endY) {
        document.querySelectorAll('.square.last-move').forEach(el => el.classList.remove('last-move'));
        document.querySelector(`.square[data-x="${startX}"][data-y="${startY}"]`).classList.add('last-move');
        document.querySelector(`.square[data-x="${endX}"][data-y="${endY}"]`).classList.add('last-move');
    }
 
    boardElement.addEventListener('click', function(event) {
        if (!isGameActive || !isPlayerTurn) return; 

        let clickedEl = event.target;
        if (clickedEl.classList.contains('piece')) {
            clickedEl = clickedEl.parentNode;  
        } else if (!clickedEl.classList.contains('square')) {
            return;  
        }

        const clickedX = parseInt(clickedEl.dataset.x);
        const clickedY = parseInt(clickedEl.dataset.y);
        const pieceAtClicked = currentBoardState[`${clickedY},${clickedX}`]; 

        if (!selectedSquare) { 
            if (pieceAtClicked && pieceAtClicked.endsWith(playerColor)) {  
                selectedSquare = clickedEl;
                selectedSquare.classList.add('selected');
                showValidMoves(clickedX, clickedY);
            }
        } else {
 
            const startX = parseInt(selectedSquare.dataset.x);
            const startY = parseInt(selectedSquare.dataset.y);

            if (selectedSquare === clickedEl) {
             
                clearHighlights();
                selectedSquare = null;
            } else if (clickedEl.classList.contains('valid-move-dot')) {
            
                handlePlayerMove(startX, startY, clickedX, clickedY);
                clearHighlights();
                selectedSquare = null;
            } else if (pieceAtClicked && pieceAtClicked.endsWith(playerColor)) {
       
                clearHighlights();
                selectedSquare = clickedEl;
                selectedSquare.classList.add('selected');
                showValidMoves(clickedX, clickedY);
            } else {
         
                clearHighlights();
                selectedSquare = null;
            }
        }
    });
 
    resignBtn.addEventListener('click', function() {
        if (!isGameActive) return;
        if (confirm('Bạn có chắc chắn muốn đầu hàng ván cờ này không?')) {
            fetch(`${contextPath}/api/ai/resign`, { 
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    endGame('RESIGN');
                } else {
                    statusMessageEl.textContent = `Lỗi: ${data.message || 'Không thể đầu hàng.'}`;
                }
            })
            .catch(error => {
                console.error('Lỗi khi đầu hàng:', error);
                statusMessageEl.textContent = 'Lỗi server khi đầu hàng.';
            });
        }
    });

    // khởi tạo game
    function initializeAiGame() { 
        if (!initialBoardState) {
            statusMessageEl.textContent = "Lỗi: Không tìm thấy trạng thái ván cờ ban đầu.";
            isGameActive = false;
            return;
        }

        renderBoardFromState(initialBoardState);
        updateCapturedPieces(initialBoardState);  
		
        if (aiColor === 'Red') {
            isPlayerTurn = false;
            updateTurnIndicator();
            setTimeout(() => {
 
                fetch(`${contextPath}/api/ai/move`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: JSON.stringify({ startX: -1, startY: -1, endX: -1, endY: -1 }) 
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        handleAiMove(data.aiMove);
                        renderBoardFromState(data.newBoardState);
                        updateCapturedPieces(data.newBoardState);
                        sounds.move.play();
                        highlightLastMove(data.aiMove.startX, data.aiMove.startY, data.aiMove.endX, data.aiMove.endY);

                        if (data.gameState !== 'IN_PROGRESS') {
                            endGame(data.gameState);
                            return;
                        }

                        isPlayerTurn = true;
                        updateTurnIndicator();
                    } else {
                        statusMessageEl.textContent = `Lỗi: ${data.message || 'Máy không thể đi.'}`;
                        isGameActive = false;
                    }
                })
                .catch(error => {
                    console.error('Lỗi khi máy đi nước đầu:', error);
                    statusMessageEl.textContent = 'Lỗi server khi máy đi nước đầu.';
                    isGameActive = false;
                });
            }, 1000);
        } else {
            isPlayerTurn = true;
            updateTurnIndicator();
        }

        window.addEventListener('resize', updateSquarePositions);
        updateSquarePositions();  
        statusMessageEl.textContent = isPlayerTurn ? 'Đến lượt bạn!' : 'Máy đang suy nghĩ...';
    }
 
    initializeAiGame();
 
    window.addEventListener('beforeunload', function() {
        sessionStorage.removeItem('ai_initial_board');
    });
});