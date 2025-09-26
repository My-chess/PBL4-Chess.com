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
            if (y <= 4) { 
                return dx == 0 && dy == -1;
            } else {
                return (dx == 0 && dy == -1) || (dy == 0 && Math.abs(dx) == 1);
            }
        } else { 
            if (y >= 5) {
                return dx == 0 && dy == 1;
            } else {
                return (dx == 0 && dy == 1) || (dy == 0 && Math.abs(dx) == 1);
            }
        }
    }
}
