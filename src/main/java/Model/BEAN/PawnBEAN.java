package Model.BEAN;

public class PawnBEAN extends PieceBEAN {
    public PawnBEAN(int id, String color, int x, int y) {
        super(id, color, x, y);
    }

    @Override
    public boolean isValidMove(int newX, int newY, PieceBEAN[][] board) {
        int dx = newX - x;
        int dy = newY - y;
        if (color.equals("Red")) {
            // Red pawn is on its side of the river (y >= 5)
            if (y >= 5) {
                // Can only move forward
                return dx == 0 && dy == -1;
            } else { // Red pawn has crossed the river (y <= 4)
                // Can move forward or sideways
                return (dx == 0 && dy == -1) || (dy == 0 && Math.abs(dx) == 1);
            }
        } else { // Black pawn
            // Black pawn is on its side of the river (y <= 4)
            if (y <= 4) {
                // Can only move forward
                return dx == 0 && dy == 1;
            } else { // Black pawn has crossed the river (y >= 5)
                // Can move forward or sideways
                return (dx == 0 && dy == 1) || (dy == 0 && Math.abs(dx) == 1);
            }
        }
    }
}
