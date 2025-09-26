package Model.BEAN;

public class KnightBEAN extends PieceBEAN {
    public KnightBEAN(int id, String color, int x, int y) {
        super(id, color, x, y);
    }

    @Override
    public boolean isValidMove(int newX, int newY, PieceBEAN[][] board) {
        int dx = Math.abs(newX - x);
        int dy = Math.abs(newY - y);
        if (!((dx == 2 && dy == 1) || (dx == 1 && dy == 2))) return false;
        if (dx == 2) {
            int blockX = (newX + x) / 2;
            if (board[y][blockX] != null) return false;
        } else {
            int blockY = (newY + y) / 2;
            if (board[blockY][x] != null) return false;
        }

        return true;
    }
}
