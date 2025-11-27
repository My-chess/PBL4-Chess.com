package Model.BEAN; // Giữ nguyên package của bạn

import java.util.Map;
import Model.DAO.FirebaseService;

import Model.BEAN.PieceBEAN; // Đảm bảo bạn đã import các lớp quân cờ

/**
 * Lớp Board đại diện cho bàn cờ và chứa toàn bộ logic của ván đấu.
 * Nó quản lý trạng thái của tất cả các quân cờ, xác thực các nước đi,
 * và kiểm tra các điều kiện kết thúc game như chiếu và chiếu bí.
 */
public class Board {

    // Mảng 2 chiều 10x9 đại diện cho bàn cờ. grid[y][x]
    private final PieceBEAN[][] grid;

    /**
     * Hàm dựng, khởi tạo bàn cờ với kích thước 10x9 và
     * gọi hàm để sắp xếp các quân cờ vào vị trí ban đầu.
     */
    public Board() {
        this.grid = new PieceBEAN[10][9];
        setupInitialPieces();
    }
    
    public Board(Map<String, String> boardState) {
        this.grid = new PieceBEAN[10][9];
        if (boardState == null) return;

        for (Map.Entry<String, String> entry : boardState.entrySet()) {
            String[] pos = entry.getKey().split(",");
            int y = Integer.parseInt(pos[0]);
            int x = Integer.parseInt(pos[1]);

            String[] pieceInfo = entry.getValue().split("_");
            String pieceType = pieceInfo[0];
            String color = pieceInfo[1];

            // Dựa vào thông tin để tạo đối tượng quân cờ tương ứng
            switch (pieceType) {
                case "RookBEAN": grid[y][x] = new RookBEAN(0, color, x, y); break;
                case "KnightBEAN": grid[y][x] = new KnightBEAN(0, color, x, y); break;
                case "BishopBEAN": grid[y][x] = new BishopBEAN(0, color, x, y); break;
                case "AdvisorBEAN": grid[y][x] = new AdvisorBEAN(0, color, x, y); break;
                case "KingBEAN": grid[y][x] = new KingBEAN(0, color, x, y); break;
                case "CannonBEAN": grid[y][x] = new CannonBEAN(0, color, x, y); break;
                case "PawnBEAN": grid[y][x] = new PawnBEAN(0, color, x, y); break;
            }
        }
    }
    
    public Board(Board other) {
        this(Model.DAO.FirebaseService.boardToMap(other));
    }

    /**
     * Sắp xếp 32 quân cờ vào vị trí xuất phát theo luật cờ tướng.
     */
    private void setupInitialPieces() {
        // Quân Đen
        grid[0][0] = new RookBEAN(1, "Black", 0, 0);
        grid[0][1] = new KnightBEAN(2, "Black", 1, 0);
        grid[0][2] = new BishopBEAN(3, "Black", 2, 0);
        grid[0][3] = new AdvisorBEAN(4, "Black", 3, 0);
        grid[0][4] = new KingBEAN(5, "Black", 4, 0);
        grid[0][5] = new AdvisorBEAN(6, "Black", 5, 0);
        grid[0][6] = new BishopBEAN(7, "Black", 6, 0);
        grid[0][7] = new KnightBEAN(8, "Black", 7, 0);
        grid[0][8] = new RookBEAN(9, "Black", 8, 0);
        grid[2][1] = new CannonBEAN(10, "Black", 1, 2);
        grid[2][7] = new CannonBEAN(11, "Black", 7, 2);
        grid[3][0] = new PawnBEAN(12, "Black", 0, 3);
        grid[3][2] = new PawnBEAN(13, "Black", 2, 3);
        grid[3][4] = new PawnBEAN(14, "Black", 4, 3);
        grid[3][6] = new PawnBEAN(15, "Black", 6, 3);
        grid[3][8] = new PawnBEAN(16, "Black", 8, 3);

        // Quân Đỏ
        grid[9][0] = new RookBEAN(17, "Red", 0, 9);
        grid[9][1] = new KnightBEAN(18, "Red", 1, 9);
        grid[9][2] = new BishopBEAN(19, "Red", 2, 9);
        grid[9][3] = new AdvisorBEAN(20, "Red", 3, 9);
        grid[9][4] = new KingBEAN(21, "Red", 4, 9);
        grid[9][5] = new AdvisorBEAN(22, "Red", 5, 9);
        grid[9][6] = new BishopBEAN(23, "Red", 6, 9);
        grid[9][7] = new KnightBEAN(24, "Red", 7, 9);
        grid[9][8] = new RookBEAN(25, "Red", 8, 9);
        grid[7][1] = new CannonBEAN(26, "Red", 1, 7);
        grid[7][7] = new CannonBEAN(27, "Red", 7, 7);
        grid[6][0] = new PawnBEAN(28, "Red", 0, 6);
        grid[6][2] = new PawnBEAN(29, "Red", 2, 6);
        grid[6][4] = new PawnBEAN(30, "Red", 4, 6);
        grid[6][6] = new PawnBEAN(31, "Red", 6, 6);
        grid[6][8] = new PawnBEAN(32, "Red", 8, 6);
    }

    /**
     * Lấy quân cờ tại một tọa độ (x, y) cụ thể.
     * @param x Tọa độ x (cột).
     * @param y Tọa độ y (hàng).
     * @return Đối tượng PieceBEAN nếu có, ngược lại trả về null.
     */
    public PieceBEAN getPieceAt(int x, int y) {
        if (!isWithinBounds(x, y)) {
            return null;
        }
        return grid[y][x];
    }

    /**
     * Lấy toàn bộ mảng quân cờ.
     * @return Mảng 2 chiều PieceBEAN.
     */
    public PieceBEAN[][] getGrid() {
        return this.grid;
    }

    /**
     * Kiểm tra xem một tọa độ có nằm trong bàn cờ không.
     * @param x Tọa độ x.
     * @param y Tọa độ y.
     * @return true nếu hợp lệ, false nếu không.
     */
    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 9 && y >= 0 && y < 10;
    }

    /**
     * Thực hiện một nước đi trên bàn cờ.
     * Hàm này thay đổi trực tiếp trạng thái của `grid`.
     * @param startX Vị trí bắt đầu X.
     * @param startY Vị trí bắt đầu Y.
     * @param endX Vị trí kết thúc X.
     * @param endY Vị trí kết thúc Y.
     */
    public void executeMove(int startX, int startY, int endX, int endY) {
        // 1. Kiểm tra an toàn: Nếu tọa độ nằm ngoài bàn cờ thì không làm gì cả
        if (!isWithinBounds(startX, startY) || !isWithinBounds(endX, endY)) {
            System.err.println("Lỗi: Cố gắng di chuyển ra ngoài bàn cờ! [" + startX + "," + startY + "] -> [" + endX + "," + endY + "]");
            return;
        }

        PieceBEAN pieceToMove = getPieceAt(startX, startY);
        if (pieceToMove != null) {
            PieceBEAN capturedPiece = getPieceAt(endX, endY);
            if (capturedPiece != null) {
                capturedPiece.setAlive(false); 
            }
            pieceToMove.setPosition(endX, endY);
            grid[endY][endX] = pieceToMove;
            grid[startY][startX] = null;
        }
    }
    
    /**
     * >>> HOÀN THIỆN LOGIC KIỂM TRA NƯỚC ĐI <<<
     * Đây là hàm kiểm tra toàn diện nhất, bao gồm tất cả các luật cờ.
     * @param startX Vị trí bắt đầu X.
     * @param startY Vị trí bắt đầu Y.
     * @param endX Vị trí kết thúc X.
     * @param endY Vị trí kết thúc Y.
     * @return true nếu nước đi hợp lệ, false nếu không.
     */
    public boolean isMoveValid(int startX, int startY, int endX, int endY) {
    	if (!isWithinBounds(startX, startY) || !isWithinBounds(endX, endY)) {
            return false;
        }
    	
        PieceBEAN piece = getPieceAt(startX, startY);
        if (piece == null) return false;
        
        PieceBEAN destinationPiece = getPieceAt(endX, endY);
        if (destinationPiece != null && destinationPiece.getColor().equals(piece.getColor())) {
            return false;
        }
        // Logic cơ bản: đi đúng luật của quân cờ
        if (!piece.isValidMove(endX, endY, grid)) return false;

        // Giả lập nước đi để kiểm tra các luật cấm
        PieceBEAN target = grid[endY][endX]; // Lưu quân bị ăn (nếu có)
        
        // Thực hiện đi thử
        grid[endY][endX] = piece;
        grid[startY][startX] = null;
        piece.setPosition(endX, endY);

        boolean isValid = true;

        // A. Kiểm tra luật Tướng không được đối mặt
        if (areKingsFacing()) {
            isValid = false;
        }
        
        // B. Kiểm tra luật Tướng không được bị chiếu sau khi đi (Tự sát)
        // (Lưu ý: Hàm isKingInCheck bạn đã có từ trước, đảm bảo nó kiểm tra đúng màu)
        else if (isKingInCheck(piece.getColor())) {
            isValid = false;
        }

        // Hoàn tác nước đi (Trả lại hiện trạng cũ)
        piece.setPosition(startX, startY);
        grid[startY][startX] = piece;
        grid[endY][endX] = target;

        return isValid;
    }
    
    public boolean hasLegalMoves(String color) {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = grid[y][x];
                if (p != null && p.getColor().equals(color)) {
                    // Thử tất cả các nước đi có thể của quân này
                    for (int ty = 0; ty < 10; ty++) {
                        for (int tx = 0; tx < 9; tx++) {
                            if (isMoveValid(x, y, tx, ty)) {
                                return true; // Chỉ cần tìm thấy 1 nước đi hợp lệ là còn sống
                            }
                        }
                    }
                }
            }
        }
        return false; // Không còn nước đi nào -> Thua
    }
    
    /**
     * Kiểm tra xem Tướng của một màu cụ thể có đang bị chiếu hay không.
     * @param kingColor Màu của Tướng cần kiểm tra ("Red" hoặc "Black").
     * @return true nếu Tướng đang bị chiếu.
     */
    public boolean isKingInCheck(String kingColor) {
        int kingX = -1, kingY = -1;
        // 1. Tìm vị trí của Tướng
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = getPieceAt(x, y);
                if (p instanceof KingBEAN && p.getColor().equals(kingColor)) {
                    kingX = x;
                    kingY = y;
                    break;
                }
            }
            if (kingX != -1) break;
        }

        if (kingX == -1) return false; // Không tìm thấy Tướng (trường hợp bất thường)

        // 2. Duyệt qua tất cả quân cờ của đối phương.
        String opponentColor = kingColor.equals("Red") ? "Black" : "Red";
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = getPieceAt(x, y);
                if (p != null && p.getColor().equals(opponentColor)) {
                    // Kiểm tra xem quân cờ này có thể di chuyển hợp lệ đến vị trí Tướng không.
                    // Lưu ý: isValidMove của từng quân đã bao gồm logic ăn quân.
                    if (p.isValidMove(kingX, kingY, this.grid)) {
                        return true; // Tìm thấy một quân có thể ăn Tướng -> Tướng bị chiếu.
                    }
                }
            }
        }
        return false; // Duyệt hết bàn cờ mà không có quân nào chiếu Tướng.
    }

    /**
     * Kiểm tra xem hai Tướng có đang đối mặt nhau trên cùng một cột mà không có quân cản không.
     * @return true nếu hai Tướng đang đối mặt.
     */
    public boolean areKingsFacing() {
        PieceBEAN redKing = null;
        PieceBEAN blackKing = null;

        // Tìm vị trí 2 tướng
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = grid[y][x];
                if (p instanceof KingBEAN) {
                    if (p.getColor().equals("Red")) redKing = p;
                    else blackKing = p;
                }
            }
        }

        if (redKing == null || blackKing == null) return false; // Lỗi dữ liệu (không nên xảy ra)

        // Nếu không cùng cột (X khác nhau) thì không sao
        if (redKing.getX() != blackKing.getX()) return false;

        // Nếu cùng cột, kiểm tra xem có quân nào chắn giữa không
        int minY = Math.min(redKing.getY(), blackKing.getY());
        int maxY = Math.max(redKing.getY(), blackKing.getY());
        int count = 0;

        for (int y = minY + 1; y < maxY; y++) {
            if (grid[y][redKing.getX()] != null) {
                count++;
            }
        }

        return count == 0; // Nếu không có quân chắn -> Tướng đối mặt (Phạm quy)
    }
    
    /**
     * Kiểm tra xem một bên có bị chiếu bí hay không.
     * @param color Màu của bên bị kiểm tra ("Red" hoặc "Black").
     * @return true nếu bên đó đã bị chiếu bí.
     */
    public boolean isCheckmate(String color) {
        // 1. Nếu Tướng không bị chiếu, không thể là chiếu bí.
        if (!isKingInCheck(color)) {
            return false;
        }

        // 2. Duyệt qua tất cả các quân cờ của bên đang bị chiếu.
        for (int startY = 0; startY < 10; startY++) {
            for (int startX = 0; startX < 9; startX++) {
                PieceBEAN piece = getPieceAt(startX, startY);
                if (piece != null && piece.getColor().equals(color)) {
                    // 3. Thử tất cả các nước đi có thể có của quân cờ này.
                    for (int endY = 0; endY < 10; endY++) {
                        for (int endX = 0; endX < 9; endX++) {
                            // Dùng hàm isMoveValid đã được hoàn thiện.
                            // Nó đã bao gồm cả việc kiểm tra xem nước đi thử có tự làm Tướng bị chiếu không.
                            if (isMoveValid(startX, startY, endX, endY)) {
                                // Nếu tìm thấy DÙ CHỈ MỘT nước đi hợp lệ để thoát chiếu,
                                // thì đây không phải là chiếu bí.
                                return false;
                            }
                        }
                    }
                }
            }
        }

        // Nếu đã thử hết tất cả các quân cờ và tất cả các nước đi mà vẫn bị chiếu,
        // thì đó chính là chiếu bí.
        return true;
    }
    
    /**
     * Hoàn tác một nước đi. Hữu ích cho việc mô phỏng và kiểm tra.
     * @param startX Vị trí X ban đầu.
     * @param startY Vị trí Y ban đầu.
     * @param endX Vị trí X đã di chuyển tới.
     * @param endY Vị trí Y đã di chuyển tới.
     * @param capturedPiece Quân cờ đã bị ăn (có thể là null).
     */
    private void undoMove(int startX, int startY, int endX, int endY, PieceBEAN capturedPiece) {
        PieceBEAN movedPiece = getPieceAt(endX, endY);
        if (movedPiece != null) {
            movedPiece.setPosition(startX, startY);
        }
        this.grid[startY][startX] = movedPiece;
        this.grid[endY][endX] = capturedPiece; // Phục hồi quân cờ đã bị ăn (nếu có)
    }

    // Hàm displayBoardToConsole giữ nguyên để debug nếu cần
    public void displayBoardToConsole() {
        System.out.println("   0  1  2  3  4  5  6  7  8  (x)");
        System.out.println(" +--+--+--+--+--+--+--+--+--+");
        for (int y = 0; y < 10; y++) {
            System.out.printf("%d|", y);
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = grid[y][x];
                if (p == null) {
                    System.out.print(" . ");
                } else {
                    char pieceChar = p.getClass().getSimpleName().charAt(0);
                    if (p.getColor().equals("Red")) {
                        System.out.printf(" %c ", Character.toUpperCase(pieceChar));
                    } else {
                        System.out.printf(" %c ", Character.toLowerCase(pieceChar));
                    }
                }
            }
            System.out.println("|");
        }
        System.out.println(" +--+--+--+--+--+--+--+--+--+");
    }
}