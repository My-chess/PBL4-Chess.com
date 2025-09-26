package Model.BEAN;

public class RookBEAN extends PieceBEAN {
    public RookBEAN(int id, String color, int x, int y) {
        super(id, color, x, y);
    }

    @Override
    public boolean isValidMove(int newX, int newY, PieceBEAN[][] board) {
        if (newX < 0 || newX > 8 || newY < 0 || newY > 9) return false;
        if (x != newX && y != newY) return false;
        if (!isPathClear(newX, newY, board)) return false;
        PieceBEAN target = board[newY][newX];
        if (target != null && target.getColor().equals(this.color)) return false;
        return true;
    }
    private boolean isPathClear(int newX, int newY, PieceBEAN[][] board) {
        if (x == newX) { 
            int step = (newY > y) ? 1 : -1;
            for (int i = y + step; i != newY; i += step) {
                if (board[i][x] != null) return false;
            }
        } else { 
            int step = (newX > x) ? 1 : -1;
            for (int i = x + step; i != newX; i += step) {
                if (board[y][i] != null) return false;
            }
        }
        return true;
    }
}
