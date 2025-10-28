/**
 * File: replay.js (Phiên bản tương thích ES5)
 * Chịu trách nhiệm cho toàn bộ logic của trang xem lại ván đấu.
 */

document.addEventListener('DOMContentLoaded', function() {
    // --- PHẦN 1: KHAI BÁO BIẾN (SỬ DỤNG 'var' THAY CHO 'const' VÀ 'let') ---
    var boardElement = document.getElementById('board');
    var btnStart = document.getElementById('btn-start');
    var btnPrev = document.getElementById('btn-prev');
    var btnNext = document.getElementById('btn-next');
    var btnEnd = document.getElementById('btn-end');
    var moveCounterEl = document.getElementById('move-counter');
    var totalMovesEl = document.getElementById('total-moves');
    var moveDescriptionEl = document.getElementById('move-description');

    // --- PHẦN 2: QUẢN LÝ TRẠNG THÁI ---
    var allMoves = [];
    var boardStates = [];
    var currentMoveIndex = -1;

    // --- PHẦN 3: CÁC HÀM XỬ LÝ LOGIC BÀN CỜ ---

    /**
     * Trả về đối tượng trạng thái bàn cờ ban đầu.
     */
    function getInitialBoardState() {
        return {
            "0,0": "RookBEAN_Black", "0,1": "KnightBEAN_Black", "0,2": "BishopBEAN_Black", "0,3": "AdvisorBEAN_Black", "0,4": "KingBEAN_Black", "0,5": "AdvisorBEAN_Black", "0,6": "BishopBEAN_Black", "0,7": "KnightBEAN_Black", "0,8": "RookBEAN_Black",
            "2,1": "CannonBEAN_Black", "2,7": "CannonBEAN_Black",
            "3,0": "PawnBEAN_Black", "3,2": "PawnBEAN_Black", "3,4": "PawnBEAN_Black", "3,6": "PawnBEAN_Black", "3,8": "PawnBEAN_Black",
            
            "9,0": "RookBEAN_Red", "9,1": "KnightBEAN_Red", "9,2": "BishopBEAN_Red", "9,3": "AdvisorBEAN_Red", "9,4": "KingBEAN_Red", "9,5": "AdvisorBEAN_Red", "9,6": "BishopBEAN_Red", "9,7": "KnightBEAN_Red", "9,8": "RookBEAN_Red",
            "7,1": "CannonBEAN_Red", "7,7": "CannonBEAN_Red",
            "6,0": "PawnBEAN_Red", "6,2": "PawnBEAN_Red", "6,4": "PawnBEAN_Red", "6,6": "PawnBEAN_Red", "6,8": "PawnBEAN_Red"
        };
    }

    /* ===== SỬA: Hàm sao chép đối tượng tương thích ES5 (thay cho Object.assign) ===== */
    function cloneBoardState(boardState) {
        var newBoardState = {};
        for (var key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                newBoardState[key] = boardState[key];
            }
        }
        return newBoardState;
    }

    /**
     * Thực hiện một nước đi và trả về trạng thái bàn cờ mới.
     */
    function executeMove(boardState, move) {
        /* ===== SỬA: Sử dụng hàm sao chép ES5 ===== */
        var newBoardState = cloneBoardState(boardState);
        
        var startKey = move.startY + ',' + move.startX;
        var endKey = move.endY + ',' + move.endX;
        
        if (newBoardState[startKey]) {
            // Logic di chuyển:
            // 1. Gán quân cờ ở vị trí mới (ghi đè nếu có ăn quân)
            newBoardState[endKey] = newBoardState[startKey];
            // 2. Xóa quân cờ ở vị trí cũ
            delete newBoardState[startKey];
        }
        return newBoardState;
    }

    /**
     * Vẽ lại toàn bộ bàn cờ từ một đối tượng trạng thái.
     * ===== SỬA: VIẾT LẠI HOÀN TOÀN HÀM NÀY ĐỂ GIỐNG VỚI `game.js` (dùng absolute positioning) =====
     */
    function renderBoardFromState(boardState) {
        boardElement.innerHTML = ''; // Xóa sạch các ô và quân cờ cũ

        // Lấy kích thước ô vuông từ CSS (giống hệt game.js)
        var squareSize = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--square-size'));
        if (isNaN(squareSize)) {
            squareSize = 65; // Giá trị dự phòng
        }

        // VÒNG LẶP 1: TẠO RA TẤT CẢ 90 Ô .square TRỐNG (ĐỂ ĐỊNH VỊ)
        for (var y = 0; y < 10; y++) {
            for (var x = 0; x < 9; x++) {
                var square = document.createElement('div');
                square.className = 'square';
                square.dataset.x = x;
                square.dataset.y = y;
                
                // Định vị tất cả các ô (quan trọng nhất)
                square.style.left = (x * squareSize) + 'px';
                square.style.top = (y * squareSize) + 'px';
                
                boardElement.appendChild(square);
            }
        }

        // VÒNG LẶP 2: ĐẶT CÁC QUÂN CỜ (IMG) VÀO CÁC Ô .square TƯƠNG ỨNG
        for (var key in boardState) {
            if (boardState.hasOwnProperty(key)) {
                var pos = key.split(',');
                var y = parseInt(pos[0]);
                var x = parseInt(pos[1]);
                var pieceName = boardState[key];

                // Tìm ô .square đã được tạo ở trên
                var selector = '.square[data-x="' + x + '"][data-y="' + y + '"]';
                var targetSquare = boardElement.querySelector(selector);

                if (targetSquare) {
                    var pieceElement = document.createElement('img');
                    // Thêm class 'piece' để CSS áp dụng style (bo tròn, bóng đổ, v.v.)
                    pieceElement.className = 'piece'; 
                    pieceElement.src = contextPath + '/images/' + pieceName + '.png';
                    pieceElement.alt = pieceName;
                    
                    // Thêm quân cờ (img) vào ô (div)
                    targetSquare.appendChild(pieceElement);
                }
            }
        }
    }
    
    // --- PHẦN 4: HÀM CẬP NHẬT GIAO DIỆN TỔNG THỂ ---
    function updateUI() {
        var currentState = boardStates[currentMoveIndex + 1];
        renderBoardFromState(currentState);
        
        moveCounterEl.textContent = currentMoveIndex + 1;
        
        btnStart.disabled = (currentMoveIndex === -1);
        btnPrev.disabled = (currentMoveIndex === -1);
        btnNext.disabled = (currentMoveIndex === allMoves.length - 1);
        btnEnd.disabled = (currentMoveIndex === allMoves.length - 1);

        if (currentMoveIndex >= 0) {
            var move = allMoves[currentMoveIndex];
            // Lấy tên quân cờ từ boardState để đảm bảo chính xác (ví dụ: RookBEAN_Red)
            var pieceKey = move.endY + ',' + move.endX;
            var pieceName = currentState[pieceKey] || "Unknown"; // Lấy tên quân cờ ở vị trí MỚI
            
            var pieceDisplayName = pieceName.split('_')[0].replace('BEAN', '');
            var colorName = pieceName.split('_')[1] === 'Red' ? 'Đỏ' : 'Đen';

            moveDescriptionEl.textContent = 'Nước ' + (currentMoveIndex + 1) + ': Phe ' + colorName + ' đi ' + pieceDisplayName + ' từ [' + move.startX + ', ' + move.startY + '] đến [' + move.endX + ', ' + move.endY + ']';
        } else {
            moveDescriptionEl.textContent = "Trạng thái ban đầu của ván cờ.";
        }
    }

    // --- PHẦN 5: GÁN SỰ KIỆN CHO CÁC NÚT ĐIỀU KHIỂN ---
    btnStart.addEventListener('click', function() {
        currentMoveIndex = -1;
        updateUI();
    });

    btnPrev.addEventListener('click', function() {
        if (currentMoveIndex > -1) {
            currentMoveIndex--;
            updateUI();
        }
    });

    btnNext.addEventListener('click', function() {
        if (currentMoveIndex < allMoves.length - 1) {
            currentMoveIndex++;
            updateUI();
        }
    });
    
    btnEnd.addEventListener('click', function() {
        currentMoveIndex = allMoves.length - 1;
        updateUI();
    });

    // --- PHẦN 6: HÀM KHỞI TẠO CHÍNH ---

    /**
     * ===== SỬA: Thay thế async/await và fetch() bằng XMLHttpRequest (ES5) =====
     */
    function initializeReplay() {
        // Bước 1: Tạo đối tượng XMLHttpRequest
        var xhr = new XMLHttpRequest();
        var url = contextPath + '/api/moves?matchId=' + matchId;
        
        xhr.open('GET', url, true);

        // Bước 2: Định nghĩa callback cho sự kiện onreadystatechange
        xhr.onreadystatechange = function() {
            // Chỉ thực thi khi yêu cầu hoàn tất (readyState === 4)
            if (xhr.readyState === 4) {
                // Trường hợp thành công (status === 200)
                if (xhr.status === 200) {
                    try {
                        var moves = JSON.parse(xhr.responseText);
                        
                        // Bước 3: Xử lý dữ liệu (giống .then() cũ)
                        allMoves = moves;
                        totalMovesEl.textContent = allMoves.length;

                        // Tính toán trước tất cả các trạng thái bàn cờ
                        boardStates.push(getInitialBoardState());
                        var currentBoard = getInitialBoardState();
                        for (var i = 0; i < allMoves.length; i++) {
                            var move = allMoves[i];
                            currentBoard = executeMove(currentBoard, move);
                            boardStates.push(currentBoard);
                        }

                        // Bước 4: Hiển thị giao diện ở trạng thái ban đầu
                        updateUI();
                        
                    } catch (e) {
                        // Lỗi parse JSON
                        console.error('Lỗi phân tích dữ liệu JSON:', e);
                        moveDescriptionEl.textContent = 'Lỗi: Dữ liệu ván cờ không hợp lệ.';
                    }
                } 
                // Trường hợp lỗi (status !== 200)
                else {
                    // Bước 3 (Lỗi): Xử lý lỗi (giống .catch() cũ)
                    console.error('Không thể tải lịch sử ván cờ. Status:', xhr.status);
                    moveDescriptionEl.textContent = 'Lỗi: ' + xhr.statusText + '. Không thể tải dữ liệu ván cờ.';
                }
            }
        };

        // Bước 5: Gửi yêu cầu
        xhr.send();
    }

    // Chạy hàm khởi tạo
    initializeReplay();
});