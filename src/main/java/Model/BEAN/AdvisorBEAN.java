package Model.BEAN;

public class AdvisorBEAN extends PieceBEAN {
    public AdvisorBEAN(int id, String color, int x, int y) {
        super(id, color, x, y);
    }
    @Override
    public boolean isValidMove(int newX, int newY, PieceBEAN[][] board) {
        int dx = Math.abs(newX - x);
        int dy = Math.abs(newY - y);
        if (dx != 1 || dy != 1) return false;
        if (color.equals("Red")) {
            return (newX >= 3 && newX <= 5 && newY >= 7 && newY <= 9);
        } else {
            return (newX >= 3 && newX <= 5 && newY >= 0 && newY <= 2);
        }
    }
}