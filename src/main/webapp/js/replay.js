/**
 * File: replay.js (Phiên bản cập nhật)
 * - Vẽ quân cờ bằng CSS/Text thay vì ảnh.
 * - Lấy và hiển thị thông tin người chơi.
 */

document.addEventListener('DOMContentLoaded', function() {
    // --- PHẦN 1: KHAI BÁO BIẾN ---
    var boardElement = document.getElementById('board');
    var btnStart = document.getElementById('btn-start');
    var btnPrev = document.getElementById('btn-prev');
    var btnNext = document.getElementById('btn-next');
    var btnEnd = document.getElementById('btn-end');
    var moveCounterEl = document.getElementById('move-counter');
    var totalMovesEl = document.getElementById('total-moves');
    var moveDescriptionEl = document.getElementById('move-description');
    
    // Lấy các element thông tin người chơi
    var playerRedNameEl = document.getElementById('player-red-name');
    var playerRedEloEl = document.getElementById('player-red-elo');
    var playerBlackNameEl = document.getElementById('player-black-name');
    var playerBlackEloEl = document.getElementById('player-black-elo');

    // --- PHẦN 2: QUẢN LÝ TRẠNG THÁI ---
    var allMoves = [];
    var boardStates = [];
    var currentMoveIndex = -1;

    // THAY ĐỔI: Thêm các map ký tự quân cờ (giống hệt game.js)
    var pieceToChar = { "KingBEAN": "將", "AdvisorBEAN": "士", "BishopBEAN": "象", "KnightBEAN": "馬", "RookBEAN": "車", "CannonBEAN": "炮", "PawnBEAN": "卒" };
    var redPieceToChar = { "KingBEAN": "帥", "AdvisorBEAN": "仕", "BishopBEAN": "相", "KnightBEAN": "傌", "RookBEAN": "俥", "CannonBEAN": "砲", "PawnBEAN": "兵" };


    // --- PHẦN 3: CÁC HÀM XỬ LÝ LOGIC BÀN CỜ ---

    function getInitialBoardState() {
        return { "0,0": "RookBEAN_Black", "0,1": "KnightBEAN_Black", "0,2": "BishopBEAN_Black", "0,3": "AdvisorBEAN_Black", "0,4": "KingBEAN_Black", "0,5": "AdvisorBEAN_Black", "0,6": "BishopBEAN_Black", "0,7": "KnightBEAN_Black", "0,8": "RookBEAN_Black", "2,1": "CannonBEAN_Black", "2,7": "CannonBEAN_Black", "3,0": "PawnBEAN_Black", "3,2": "PawnBEAN_Black", "3,4": "PawnBEAN_Black", "3,6": "PawnBEAN_Black", "3,8": "PawnBEAN_Black", "9,0": "RookBEAN_Red", "9,1": "KnightBEAN_Red", "9,2": "BishopBEAN_Red", "9,3": "AdvisorBEAN_Red", "9,4": "KingBEAN_Red", "9,5": "AdvisorBEAN_Red", "9,6": "BishopBEAN_Red", "9,7": "KnightBEAN_Red", "9,8": "RookBEAN_Red", "7,1": "CannonBEAN_Red", "7,7": "CannonBEAN_Red", "6,0": "PawnBEAN_Red", "6,2": "PawnBEAN_Red", "6,4": "PawnBEAN_Red", "6,6": "PawnBEAN_Red", "6,8": "PawnBEAN_Red" };
    }

    function cloneBoardState(boardState) {
        var newBoardState = {};
        for (var key in boardState) { if (boardState.hasOwnProperty(key)) { newBoardState[key] = boardState[key]; } }
        return newBoardState;
    }

    function executeMove(boardState, move) {
        var newBoardState = cloneBoardState(boardState);
        var startKey = move.startY + ',' + move.startX;
        var endKey = move.endY + ',' + move.endX;
        if (newBoardState[startKey]) {
            newBoardState[endKey] = newBoardState[startKey];
            delete newBoardState[startKey];
        }
        return newBoardState;
    }

    /**
     * === THAY ĐỔI CỐT LÕI: VIẾT LẠI HOÀN TOÀN HÀM NÀY ĐỂ VẼ CỜ BẰNG CSS/TEXT ===
     */
    function renderBoardFromState(boardState) {
        boardElement.innerHTML = '';
        var squareSize = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--square-size')) || 65;

        // Vòng 1: Tạo các ô .square để định vị
        for (var y = 0; y < 10; y++) {
            for (var x = 0; x < 9; x++) {
                var square = document.createElement('div');
                square.className = 'square';
                square.dataset.x = x;
                square.dataset.y = y;
                square.style.left = (x * squareSize) + 'px';
                square.style.top = (y * squareSize) + 'px';
                boardElement.appendChild(square);
            }
        }

        // Vòng 2: Đặt các quân cờ (div) vào các ô .square
        for (var key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                var pos = key.split(',');
                var y = parseInt(pos[0]);
                var x = parseInt(pos[1]);
                var pieceInfo = boardState[key];
                var parts = pieceInfo.split('_');
                var pieceType = parts[0];
                var color = parts[1];

                var selector = '.square[data-x="' + x + '"][data-y="' + y + '"]';
                var targetSquare = boardElement.querySelector(selector);

                if (targetSquare) {
                    var pieceElement = document.createElement('div');
                    pieceElement.className = 'piece ' + color.toLowerCase();
                    // Lấy ký tự chữ Hán từ map
                    pieceElement.textContent = (color === 'Red') ? redPieceToChar[pieceType] : pieceToChar[pieceType];
                    targetSquare.appendChild(pieceElement);
                }
            }
        }
    }
    
    // --- PHẦN 4: HÀM CẬP NHẬT GIAO DIỆN TỔNG THỂ ---
    function updateUI() {
        var currentState = boardStates[currentMoveIndex + 1];
        if (!currentState) return;
        renderBoardFromState(currentState);
        
        moveCounterEl.textContent = currentMoveIndex + 1;
        
        btnStart.disabled = (currentMoveIndex <= -1);
        btnPrev.disabled = (currentMoveIndex <= -1);
        btnNext.disabled = (currentMoveIndex >= allMoves.length - 1);
        btnEnd.disabled = (currentMoveIndex >= allMoves.length - 1);

        if (currentMoveIndex >= 0) {
            var move = allMoves[currentMoveIndex];
            var pieceKey = move.endY + ',' + move.endX;
            var pieceName = currentState[pieceKey] || "Unknown";
            
            var pieceDisplayName = pieceName.split('_')[0].replace('BEAN', '');
            var colorName = pieceName.split('_')[1] === 'Red' ? 'Đỏ' : 'Đen';

            moveDescriptionEl.textContent = 'Nước ' + (currentMoveIndex + 1) + ': Phe ' + colorName + ' đi ' + pieceDisplayName + ' từ [' + move.startX + ', ' + move.startY + '] đến [' + move.endX + ', ' + move.endY + ']';
        } else {
            moveDescriptionEl.textContent = "Trạng thái ban đầu của ván cờ.";
        }
    }

    // --- PHẦN 5: GÁN SỰ KIỆN CHO CÁC NÚT ĐIỀU KHIỂN ---
    btnStart.addEventListener('click', function() { currentMoveIndex = -1; updateUI(); });
    btnPrev.addEventListener('click', function() { if (currentMoveIndex > -1) { currentMoveIndex--; updateUI(); } });
    btnNext.addEventListener('click', function() { if (currentMoveIndex < allMoves.length - 1) { currentMoveIndex++; updateUI(); } });
    btnEnd.addEventListener('click', function() { currentMoveIndex = allMoves.length - 1; updateUI(); });

    // --- PHẦN 6: HÀM KHỞI TẠO CHÍNH ---
    
    /**
     * THÊM MỚI: Hàm lấy thông tin chi tiết của trận đấu.
     * Giả sử bạn có một API endpoint là /api/matchDetails?matchId=...
     * Nếu chưa có, bạn cần tạo servlet này.
     */
    function fetchMatchDetails() {
        var xhr = new XMLHttpRequest();
        // Giả sử bạn có API này để lấy thông tin player1, player2
        var url = contextPath + '/api/matchDetails?matchId=' + matchId; 
        xhr.open('GET', url, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4 && xhr.status === 200) {
                try {
                    var matchData = JSON.parse(xhr.responseText);
                    var p1 = matchData.player1;
                    var p2 = matchData.player2;

                    if (p1) {
                        playerRedNameEl.textContent = p1.displayName;
                        playerRedEloEl.textContent = 'ELO: ' + p1.elo;
                    }
                    if (p2) {
                        playerBlackNameEl.textContent = p2.displayName;
                        playerBlackEloEl.textContent = 'ELO: ' + p2.elo;
                    }
                } catch (e) {
                    console.error("Lỗi khi phân tích thông tin trận đấu:", e);
                }
            }
        };
        xhr.send();
    }

    function initializeReplay() {
        var xhr = new XMLHttpRequest();
        var url = contextPath + '/api/moves?matchId=' + matchId;
        xhr.open('GET', url, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    try {
                        var moves = JSON.parse(xhr.responseText);
                        allMoves = moves;
                        totalMovesEl.textContent = allMoves.length;

                        boardStates.push(getInitialBoardState());
                        var currentBoard = getInitialBoardState();
                        for (var i = 0; i < allMoves.length; i++) {
                            currentBoard = executeMove(currentBoard, allMoves[i]);
                            boardStates.push(currentBoard);
                        }
                        
                        // Lấy thêm thông tin người chơi
                        fetchMatchDetails(); 

                        updateUI();
                        
                    } catch (e) {
                        console.error('Lỗi phân tích dữ liệu JSON:', e);
                        moveDescriptionEl.textContent = 'Lỗi: Dữ liệu ván cờ không hợp lệ.';
                    }
                } else {
                    console.error('Không thể tải lịch sử ván cờ. Status:', xhr.status);
                    moveDescriptionEl.textContent = 'Lỗi: ' + xhr.statusText + '. Không thể tải dữ liệu ván cờ.';
                }
            }
        };
        xhr.send();
    }

    // Chạy hàm khởi tạo
    initializeReplay();
});