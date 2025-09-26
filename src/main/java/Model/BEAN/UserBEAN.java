package Model.BEAN;

public class UserBEAN {

    private String uid;       // ID duy nhất từ Firebase Authentication
    private String username;
    private String email;
    private int elo;
    private int winCount;
    private int loseCount;
    private int drawCount;

    /**
     * Hàm dựng mặc định.
     */
    public UserBEAN() {
    }

    /**
     * Hàm dựng đầy đủ.
     */
    public UserBEAN(String uid, String username, String email, int elo, int winCount, int loseCount, int drawCount) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.elo = elo;
        this.winCount = winCount;
        this.loseCount = loseCount;
        this.drawCount = drawCount;
    }

    // --- Getters and Setters ---

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(int loseCount) {
        this.loseCount = loseCount;
    }

    public int getDrawCount() {
        return drawCount;
    }

    public void setDrawCount(int drawCount) {
        this.drawCount = drawCount;
    }
}