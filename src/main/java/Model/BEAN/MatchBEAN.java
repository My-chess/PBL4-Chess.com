package Model.BEAN;

import com.google.cloud.Timestamp;

public class MatchBEAN {

    private String matchId;
    private UserBEAN player1;
    private UserBEAN player2;
    private String status;          // WAITING, IN_PROGRESS, COMPLETED
    private String currentTurn;     // "Red" hoặc "Black"
    private Timestamp startTime;
    private Timestamp lastMoveTimestamp;
    private long player1TimeLeftMs; // Thời gian còn lại tính bằng mili giây
    private long player2TimeLeftMs;
    private String winnerId;
    private String winReason;
    private java.util.List<String> participantIds;

    /**
     * Hàm dựng mặc định.
     */
    public MatchBEAN() {
    }
    
    // --- Getters and Setters ---

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public UserBEAN getPlayer1() {
        return player1;
    }

    public void setPlayer1(UserBEAN player1) {
        this.player1 = player1;
    }

    public UserBEAN getPlayer2() {
        return player2;
    }

    public void setPlayer2(UserBEAN player2) {
        this.player2 = player2;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getLastMoveTimestamp() {
        return lastMoveTimestamp;
    }

    public void setLastMoveTimestamp(Timestamp lastMoveTimestamp) {
        this.lastMoveTimestamp = lastMoveTimestamp;
    }

    public long getPlayer1TimeLeftMs() {
        return player1TimeLeftMs;
    }

    public void setPlayer1TimeLeftMs(long player1TimeLeftMs) {
        this.player1TimeLeftMs = player1TimeLeftMs;
    }

    public long getPlayer2TimeLeftMs() {
        return player2TimeLeftMs;
    }

    public void setPlayer2TimeLeftMs(long player2TimeLeftMs) {
        this.player2TimeLeftMs = player2TimeLeftMs;
    }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public String getWinReason() { return winReason; }
    public void setWinReason(String winReason) { this.winReason = winReason; }
    public java.util.List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(java.util.List<String> participantIds) { this.participantIds = participantIds; }
}