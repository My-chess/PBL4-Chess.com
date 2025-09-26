package Model.BEAN;

import com.google.cloud.Timestamp;

public class MoveBEAN {
    private String playerColor;
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private String pieceMoved;
    private Timestamp timestamp;

    /**
     * Hàm dựng mặc định.
     */
    public MoveBEAN() {
    }

    /**
     * Hàm dựng đầy đủ.
     */
    public MoveBEAN(String playerColor, int startX, int startY, int endX, int endY, String pieceMoved, Timestamp timestamp) {
        this.playerColor = playerColor;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.pieceMoved = pieceMoved;
        this.timestamp = timestamp;
    }

    // --- Getters and Setters ---
    
    public String getPlayerColor() { return playerColor; }
    public void setPlayerColor(String playerColor) { this.playerColor = playerColor; }
    public int getStartX() { return startX; }
    public void setStartX(int startX) { this.startX = startX; }
    public int getStartY() { return startY; }
    public void setStartY(int startY) { this.startY = startY; }
    public int getEndX() { return endX; }
    public void setEndX(int endX) { this.endX = endX; }
    public int getEndY() { return endY; }
    public void setEndY(int endY) { this.endY = endY; }
    public String getPieceMoved() { return pieceMoved; }
    public void setPieceMoved(String pieceMoved) { this.pieceMoved = pieceMoved; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}