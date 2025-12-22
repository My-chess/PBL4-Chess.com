// File mới: js/chess-logic.js

/**
 * Tái tạo lại một phần logic của lớp Board.java ở phía client
 * để tính toán các nước đi hợp lệ một cách tức thì.
 */

// Hàm trợ giúp để kiểm tra xem một tọa độ có hợp lệ không
function isWithinBounds(x, y) {
    return x >= 0 && x < 9 && y >= 0 && y < 10;
}
// Thay thế hàm cloneBoard cũ bằng hàm này
function cloneBoard(board) {
    var newBoard = [];
    for (var i = 0; i < board.length; i++) {
        var newRow = [];
        for (var j = 0; j < board[i].length; j++) {
            var piece = board[i][j];
            if (piece) {
                // Tạo object mới thủ công thay vì dùng { ...piece }
                newRow.push({
                    type: piece.type,
                    color: piece.color,
                    x: piece.x,
                    y: piece.y
                });
            } else {
                newRow.push(null);
            }
        }
        newBoard.push(newRow);
    }
    return newBoard;
}

function isKingInCheck(board, kingColor) {
    // A. Tìm vị trí Tướng phe mình
    let kingX = -1, kingY = -1;
    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 9; x++) {
            const p = board[y][x];
            if (p && p.type === 'KingBEAN' && p.color === kingColor) {
                kingX = x;
                kingY = y;
                break;
            }
        }
    }
    
    // Nếu không thấy tướng (trường hợp hiếm gặp), coi như không bị chiếu
    if (kingX === -1) return false;

    // B. Duyệt tất cả quân đối phương xem có quân nào ăn được Tướng không
    const enemyColor = (kingColor === 'Red') ? 'Black' : 'Red';
    
    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 9; x++) {
            const p = board[y][x];
            // Nếu là quân địch
            if (p && p.color === enemyColor) {
                // Kiểm tra xem quân địch này có thể đi vào ô của Tướng mình không
                // Lưu ý: isMoveValidForPiece đã bao gồm logic ăn quân
                if (isMoveValidForPiece(board, p, x, y, kingX, kingY)) {
                    return true; // Tướng đang bị chiếu!
                }
            }
        }
    }
    return false; // An toàn
}

function areKingsFacing(board) {
    let redKing = null;
    let blackKing = null;

    // Tìm vị trí 2 tướng
    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 9; x++) {
            const p = board[y][x];
            if (p && p.type === 'KingBEAN') {
                if (p.color === 'Red') redKing = { x, y };
                else blackKing = { x, y };
            }
        }
    }

    if (!redKing || !blackKing) return false;

    // Nếu không cùng cột dọc -> Không sao
    if (redKing.x !== blackKing.x) return false;

    // Đếm số quân chắn ở giữa
    const minY = Math.min(redKing.y, blackKing.y);
    const maxY = Math.max(redKing.y, blackKing.y);
    let obstacleCount = 0;

    for (let y = minY + 1; y < maxY; y++) {
        if (board[y][redKing.x] !== null) {
            obstacleCount++;
        }
    }

    // Nếu không có quân chắn -> Lộ mặt tướng (Vi phạm)
    return obstacleCount === 0;
}

/**
 * Hàm chính để lấy các nước đi hợp lệ.
 * @param {Array<Array<object>>} board - Bàn cờ dạng mảng 2 chiều.
 * @param {number} startX - Tọa độ X của quân cờ.
 * @param {number} startY - Tọa độ Y của quân cờ.
 * @returns {Array<Array<number>>} - Danh sách các tọa độ [x, y] hợp lệ.
 */
function getValidMoves(board, startX, startY) {
    const piece = board[startY][startX];
    if (!piece) return [];

    const validMoves = [];
    for (let y = 0; y < 10; y++) {
        for (let x = 0; x < 9; x++) {
            // 1. Kiểm tra luật di chuyển cơ bản (Xe đi thẳng, Mã đi chéo...)
            if (isMoveValidForPiece(board, piece, startX, startY, x, y)) {
                
                // 2. --- KIỂM TRA NÂNG CAO: ĐI THỬ ---
                
                // Tạo bàn cờ tạm
                const tempBoard = cloneBoard(board);
                
                // Thực hiện nước đi trên bàn cờ tạm
                tempBoard[y][x] = tempBoard[startY][startX]; // Di chuyển quân
                tempBoard[startY][startX] = null;            // Xóa vị trí cũ
                
                // A. Kiểm tra luật Chống Tướng
                if (areKingsFacing(tempBoard)) {
                    continue; // Bỏ qua nước đi này
                }

                // B. (Tùy chọn) Kiểm tra xem đi xong Tướng mình có bị chiếu không (Tự sát)
				if (isKingInCheck(tempBoard, piece.color)) {
				    continue; // Bỏ qua nước đi này
				}

                
                // Nếu vượt qua kiểm tra, thêm vào danh sách
                validMoves.push([x, y]);
            }
        }
    }
    return validMoves;
}

/**
 * Hàm tổng quát để kiểm tra một nước đi có hợp lệ không.
 */
function isMoveValidForPiece(board, piece, startX, startY, endX, endY) {
    const destinationPiece = board[endY][endX];
    if (destinationPiece && destinationPiece.color === piece.color) {
        return false; // Không thể đi vào ô có quân cùng màu
    }

    switch (piece.type) {
        case 'KingBEAN': return isValidMoveKing(piece, startX, startY, endX, endY);
        case 'AdvisorBEAN': return isValidMoveAdvisor(piece, startX, startY, endX, endY);
        case 'BishopBEAN': return isValidMoveBishop(board, piece, startX, startY, endX, endY);
        case 'KnightBEAN': return isValidMoveKnight(board, startX, startY, endX, endY);
        case 'RookBEAN': return isValidMoveRook(board, startX, startY, endX, endY);
        case 'CannonBEAN': return isValidMoveCannon(board, startX, startY, endX, endY);
        case 'PawnBEAN': return isValidMovePawn(piece, startX, startY, endX, endY);
    }
    return false;
}

// --- CÁC HÀM KIỂM TRA CHO TỪNG LOẠI QUÂN CỜ ---

function isValidMoveKing(piece, startX, startY, endX, endY) {
    const dx = Math.abs(endX - startX);
    const dy = Math.abs(endY - startY);
    if (dx + dy !== 1) return false;
    if (endX < 3 || endX > 5) return false;
    if (piece.color === 'Red') return endY >= 7 && endY <= 9;
    return endY >= 0 && endY <= 2;
}

function isValidMoveAdvisor(piece, startX, startY, endX, endY) {
    const dx = Math.abs(endX - startX);
    const dy = Math.abs(endY - startY);
    if (dx !== 1 || dy !== 1) return false;
    if (endX < 3 || endX > 5) return false;
    if (piece.color === 'Red') return endY >= 7 && endY <= 9;
    return endY >= 0 && endY <= 2;
}

function isValidMoveBishop(board, piece, startX, startY, endX, endY) {
    const dx = Math.abs(endX - startX);
    const dy = Math.abs(endY - startY);
    if (dx !== 2 || dy !== 2) return false;
    if (piece.color === 'Red' && endY < 5) return false;
    if (piece.color === 'Black' && endY > 4) return false;
    const midX = (startX + endX) / 2;
    const midY = (startY + endY) / 2;
    return !board[midY][midX]; // Không có quân cản
}

function isValidMoveKnight(board, startX, startY, endX, endY) {
    const dx = Math.abs(endX - startX);
    const dy = Math.abs(endY - startY);
    if (!((dx === 1 && dy === 2) || (dx === 2 && dy === 1))) return false;
    if (dx === 2) {
        if (board[startY][(startX + endX) / 2]) return false;
    } else { // dy === 2
        if (board[(startY + endY) / 2][startX]) return false;
    }
    return true;
}

function isValidMoveRook(board, startX, startY, endX, endY) {
    if (startX !== endX && startY !== endY) return false;
    let count = 0;
    if (startX === endX) {
        for (let i = Math.min(startY, endY) + 1; i < Math.max(startY, endY); i++) {
            if (board[i][startX]) count++;
        }
    } else {
        for (let i = Math.min(startX, endX) + 1; i < Math.max(startX, endX); i++) {
            if (board[startY][i]) count++;
        }
    }
    return count === 0;
}

function isValidMoveCannon(board, startX, startY, endX, endY) {
    if (startX !== endX && startY !== endY) return false;
    let count = 0;
    if (startX === endX) {
        for (let i = Math.min(startY, endY) + 1; i < Math.max(startY, endY); i++) {
            if (board[i][startX]) count++;
        }
    } else {
        for (let i = Math.min(startX, endX) + 1; i < Math.max(startX, endX); i++) {
            if (board[startY][i]) count++;
        }
    }
    if (board[endY][endX]) return count === 1; // Ăn quân
    return count === 0; // Di chuyển
}

function isValidMovePawn(piece, startX, startY, endX, endY) {
    const dx = endX - startX;
    const dy = endY - startY;
    if (piece.color === 'Red') {
        if (startY >= 5) return dx === 0 && dy === -1;
        return (dy === -1 && dx === 0) || (dy === 0 && Math.abs(dx) === 1);
    } else {
        if (startY <= 4) return dx === 0 && dy === 1;
        return (dy === 1 && dx === 0) || (dy === 0 && Math.abs(dx) === 1);
    }
}