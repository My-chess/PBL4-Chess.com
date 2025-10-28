// File mới: js/chess-logic.js

/**
 * Tái tạo lại một phần logic của lớp Board.java ở phía client
 * để tính toán các nước đi hợp lệ một cách tức thì.
 */

// Hàm trợ giúp để kiểm tra xem một tọa độ có hợp lệ không
function isWithinBounds(x, y) {
    return x >= 0 && x < 9 && y >= 0 && y < 10;
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
            if (isMoveValidForPiece(board, piece, startX, startY, x, y)) {
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