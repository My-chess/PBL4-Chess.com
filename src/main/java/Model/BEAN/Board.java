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

        return pieceToMove.isValidMove(endX, endY, this.grid);
    }
}