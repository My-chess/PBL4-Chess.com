package Model.BEAN;

public class BishopBEAN extends PieceBEAN {
    public BishopBEAN(int id, String color, int x, int y) {
        super(id, color, x, y);
    }

    @Override
    public boolean isValidMove(int newX, int newY, PieceBEAN[][] board) {
        int dx = Math.abs(newX - x);
        int dy = Math.abs(newY - y);
        if (dx != 2 || dy != 2) return false;
        if (color.equals("Red") && newY < 5) return false;
        if (color.equals("Black") && newY > 4) return false;
        int midX = (x + newX) / 2;
        int midY = (y + newY) / 2;
        if (board[midY][midX] != null) return false;

        return true;
    }
}
