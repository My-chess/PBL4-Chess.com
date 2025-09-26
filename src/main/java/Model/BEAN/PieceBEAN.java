package Model.BEAN;

public abstract class PieceBEAN {
    protected int id;
    protected String color; // màu quân cờ
    protected int x;       
    protected int y;       
    protected boolean alive;  // trạng thái bị hốc hay chưa=)))

    public PieceBEAN(int id, String color, int x, int y) {
        this.id = id;
        this.color = color;
        this.x = x;
        this.y = y;
        this.alive = true;
    }
    public abstract boolean isValidMove(int newX, int newY, PieceBEAN[][] board);  // check coi đi có hợp lệ k, dùng lớp abstract để lát overider lại
    // get để sài khi controller cần hoặc view cần thì hú=))
    public int getId() { return id; }
    public String getColor() { return color; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return alive; }
    // vị trí mới quân cờ
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    // đổi trạng thái của nó
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
