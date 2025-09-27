package Model.BEAN;

public class Board {

    private final PieceBEAN[][] grid;

    public Board() {
        this.grid = new PieceBEAN[10][9];
        setupInitialPieces();
    }

    private void setupInitialPieces() {
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

    // check có ai tại chổ di chuyển tới k
    public PieceBEAN getPieceAt(int x, int y) {
        if (!isWithinBounds(x, y)) {
            return null;
        }
        return grid[y][x];
    }

    // lấy mảng
    public PieceBEAN[][] getGrid() {
        return this.grid;
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 9 && y >= 0 && y < 10;
    }

    public void executeMove(int startX, int startY, int endX, int endY) {
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

    public boolean isMoveValid(int startX, int startY, int endX, int endY) {
        PieceBEAN pieceToMove = getPieceAt(startX, startY);

        if (pieceToMove == null) {
            return false;
        }

        PieceBEAN destinationPiece = getPieceAt(endX, endY);
        if (destinationPiece != null && destinationPiece.getColor().equals(pieceToMove.getColor())) {
            return false;
        }

        // Thêm kiểm tra Tướng đối mặt sau khi di chuyển
        if (pieceToMove instanceof KingBEAN) {
            if (areKingsFacing(endX, endY, pieceToMove.getColor())) {
                return false;
            }
        } else {
            if (areKingsFacing()) {
                // Trường hợp này xử lý khi một quân cờ khác di chuyển và để lộ 2 Tướng
                // Tạo một bàn cờ tạm để kiểm tra
                PieceBEAN temp = grid[endY][endX];
                grid[endY][endX] = grid[startY][startX];
                grid[startY][startX] = null;
                boolean kingsWillFace = areKingsFacing();
                // Hoàn tác di chuyển trên bàn cờ tạm
                grid[startY][startX] = grid[endY][endX];
                grid[endY][endX] = temp;
                if (kingsWillFace)
                    return false;
            }
        }

        return pieceToMove.isValidMove(endX, endY, this.grid);
    }

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
            if (kingX != -1)
                break;
        }

        if (kingX == -1)
            return false; // Không tìm thấy Tướng

        // 2. Duyệt qua các quân cờ của đối phương và kiểm tra xem có thể ăn Tướng không
        String opponentColor = kingColor.equals("Red") ? "Black" : "Red";
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = getPieceAt(x, y);
                if (p != null && p.getColor().equals(opponentColor)) {
                    if (p.isValidMove(kingX, kingY, this.grid)) {
                        return true; // Tướng đang bị chiếu
                    }
                }
            }
        }
        return false; // Tướng an toàn
    }

    public boolean areKingsFacing() {
        PieceBEAN redKing = null, blackKing = null;
        // Tìm vị trí 2 Tướng
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = getPieceAt(x, y);
                if (p instanceof KingBEAN) {
                    if (p.getColor().equals("Red"))
                        redKing = p;
                    else
                        blackKing = p;
                }
            }
        }

        if (redKing == null || blackKing == null)
            return false;

        // Nếu 2 Tướng không cùng cột, không đối mặt
        if (redKing.getX() != blackKing.getX())
            return false;

        // Kiểm tra các ô ở giữa 2 Tướng
        for (int y = blackKing.getY() + 1; y < redKing.getY(); y++) {
            if (getPieceAt(redKing.getX(), y) != null) {
                return false; // Có quân cản ở giữa
            }
        }

        return true; // Hai Tướng đang đối mặt
    }

    private boolean areKingsFacing(int newKingX, int newKingY, String movingKingColor) {
        PieceBEAN otherKing = null;
        String otherKingColor = movingKingColor.equals("Red") ? "Black" : "Red";
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN p = getPieceAt(x, y);
                if (p instanceof KingBEAN && p.getColor().equals(otherKingColor)) {
                    otherKing = p;
                    break;
                }
            }
        }

        if (otherKing == null || newKingX != otherKing.getX())
            return false;

        int startY = Math.min(newKingY, otherKing.getY());
        int endY = Math.max(newKingY, otherKing.getY());

        for (int y = startY + 1; y < endY; y++) {
            if (getPieceAt(newKingX, y) != null)
                return false;
        }
        return true;
    }
    public boolean isCheckmate(String color) {
        // 1. Nếu Tướng không bị chiếu, không thể là chiếu bí.
        if (!isKingInCheck(color)) {
            return false;
        }

        // 2. Thử tất cả các nước đi có thể có của bên 'color'.
        for (int startY = 0; startY < 10; startY++) {
            for (int startX = 0; startX < 9; startX++) {
                PieceBEAN piece = getPieceAt(startX, startY);
                if (piece != null && piece.getColor().equals(color)) {
                    // Thử di chuyển quân cờ này đến mọi ô trên bàn cờ.
                    for (int endY = 0; endY < 10; endY++) {
                        for (int endX = 0; endX < 9; endX++) {
                            if (isMoveValid(startX, startY, endX, endY)) {
                                // 3. Tạm thời thực hiện nước đi.
                                PieceBEAN capturedPiece = getPieceAt(endX, endY);
                                executeMove(startX, startY, endX, endY);

                                // 4. Kiểm tra xem Tướng có còn bị chiếu không.
                                boolean stillInCheck = isKingInCheck(color);

                                // 5. Hoàn tác nước đi.
                                undoMove(startX, startY, endX, endY, capturedPiece);

                                // Nếu tìm thấy một nước đi hợp lệ để thoát chiếu, thì không phải chiếu bí.
                                if (!stillInCheck) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nếu đã thử hết các nước đi mà vẫn bị chiếu, đó là chiếu bí.
        return true;
    }
    private void undoMove(int startX, int startY, int endX, int endY, PieceBEAN capturedPiece) {
        PieceBEAN movedPiece = getPieceAt(endX, endY);
        movedPiece.setPosition(startX, startY);
        this.grid[startY][startX] = movedPiece;
        this.grid[endY][endX] = capturedPiece; // Phục hồi quân cờ đã bị ăn (nếu có)
    }
}
