package Model.BEAN;

public class CannonBEAN extends PieceBEAN {
    public CannonBEAN(int id, String color, int x, int y) {
        super(id, color, x, y);
    }

    @Override
    public boolean isValidMove(int newX, int newY, PieceBEAN[][] board) {
        if (x != newX && y != newY) return false;
        int countBetween = 0;
        if (x == newX) { 
            int step = (newY > y) ? 1 : -1;
            for (int i = y + step; i != newY; i += step) {
                if (board[i][x] != null) countBetween++;
            }
        } else { 
            int step = (newX > x) ? 1 : -1;
            for (int i = x + step; i != newX; i += step) {
                if (board[y][i] != null) countBetween++;
            }
        }
        if (board[newY][newX] == null) return countBetween == 0;
        else return countBetween == 1;
    }
}
