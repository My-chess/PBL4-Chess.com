 package Model.BEAN; // Giữ nguyên package của bạn

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
        PieceBEAN pieceToMove = getPieceAt(startX, startY);
        if (pieceToMove != null) {
            PieceBEAN capturedPiece = getPieceAt(endX, endY);
            if (capturedPiece != null) {
                capturedPiece.setAlive(false); // Đánh dấu quân cờ bị ăn
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
        // 1. Kiểm tra cơ bản: Phải có quân cờ ở ô bắt đầu.
        PieceBEAN pieceToMove = getPieceAt(startX, startY);
        if (pieceToMove == null) {
            return false;
        }

        // 2. Kiểm tra cơ bản: Ô đích không được có quân cờ cùng màu.
        PieceBEAN destinationPiece = getPieceAt(endX, endY);
        if (destinationPiece != null && destinationPiece.getColor().equals(pieceToMove.getColor())) {
            return false;
        }

        // 3. Kiểm tra luật di chuyển riêng của từng quân cờ.
        // Ví dụ: Mã đi chữ L, Tượng đi chéo 2, v.v.
        if (!pieceToMove.isValidMove(endX, endY, this.grid)) {
            return false;
        }
        
        // 4. Kiểm tra luật chống Tướng đối mặt.
        // Tạo một bàn cờ tạm để kiểm tra trạng thái sau khi đi.
        PieceBEAN temp = grid[endY][endX];
        grid[endY][endX] = grid[startY][startX];
        grid[startY][startX] = null;
        boolean kingsWillFace = areKingsFacing();
        // Hoàn tác di chuyển trên bàn cờ tạm.
        grid[startY][startX] = grid[endY][endX];
        grid[endY][endX] = temp;
        // Nếu sau nước đi mà 2 Tướng đối mặt, nước đi không hợp lệ.
        if (kingsWillFace) {
            return false;
        }

        // 5. >>> LOGIC MỚI QUAN TRỌNG NHẤT <<<
        // Kiểm tra xem nước đi có tự đặt Tướng của mình vào thế bị chiếu không.
        // Đây là luật cờ cơ bản nhưng thường bị bỏ sót.
        
        // 5.1. Tạm thời thực hiện nước đi trên bàn cờ thật.
        PieceBEAN capturedPiece = getPieceAt(endX, endY); // Lưu lại quân cờ có thể bị ăn
        executeMove(startX, startY, endX, endY);

        // 5.2. Kiểm tra xem Tướng của bên vừa đi có bị chiếu không.
        boolean isSelfInCheck = isKingInCheck(pieceToMove.getColor());

        // 5.3. Hoàn tác lại nước đi để trả bàn cờ về trạng thái ban đầu.
        undoMove(startX, startY, endX, endY, capturedPiece);

        // 5.4. Nếu nước đi đó làm Tướng mình bị chiếu -> nước đi không hợp lệ.
        if (isSelfInCheck) {
            return false;
        }

        // Nếu vượt qua tất cả các kiểm tra trên, nước đi là hợp lệ.
        return true;
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
        PieceBEAN redKing = null, blackKing = null;
        // 1. Tìm vị trí 2 Tướng
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = getPieceAt(x, y);
                if (p instanceof KingBEAN) {
                    if (p.getColor().equals("Red")) redKing = p;
                    else blackKing = p;
                }
            }
        }

        if (redKing == null || blackKing == null) return false;

        // 2. Nếu 2 Tướng không cùng cột, chắc chắn không đối mặt.
        if (redKing.getX() != blackKing.getX()) return false;

        // 3. Kiểm tra các ô ở giữa 2 Tướng trên cùng cột.
        for (int y = blackKing.getY() + 1; y < redKing.getY(); y++) {
            if (getPieceAt(redKing.getX(), y) != null) {
                return false; // Có quân cản ở giữa -> không đối mặt.
            }
        }

        return true; // Hai Tướng cùng cột và không có quân cản.
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