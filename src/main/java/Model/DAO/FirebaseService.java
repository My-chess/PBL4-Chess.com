package Model.DAO;

import Model.BEAN.Board;
import Model.BEAN.MoveBEAN;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseService {

    private static final Firestore db = FirestoreClient.getFirestore();
    private static final long INITIAL_TIME_MS = 10 * 60 * 1000; // 10 phút

    /**
     * Tạo một trận đấu mới và trả về ID của nó.
     */
    public static String createNewMatch(String player1Id, String player1DisplayName)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("matches").document();
        DocumentSnapshot userDoc = db.collection("users").document(player1Id).get().get();
        long player1Elo = userDoc.exists() ? userDoc.getLong("elo") : 1000;

        Map<String, Object> player1 = new HashMap<>();
        player1.put("uid", player1Id);
        player1.put("displayName", player1DisplayName);
        player1.put("elo", player1Elo);

        Map<String, Object> match = new HashMap<>();
        match.put("player1", player1);
        match.put("player2", null);
        match.put("status", "WAITING");
        match.put("startTime", com.google.cloud.Timestamp.now());
        match.put("lastMoveTimestamp", com.google.cloud.Timestamp.now());
        match.put("currentTurn", "Red");
        match.put("player1TimeLeftMs", INITIAL_TIME_MS);
        match.put("player2TimeLeftMs", INITIAL_TIME_MS);

        docRef.set(match).get();
        return docRef.getId();
    }

    /**
     * Lấy DocumentSnapshot của một trận đấu.
     */
    public static DocumentSnapshot getMatch(String matchId) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = db.collection("matches").document(matchId).get().get();
        return document.exists() ? document : null;
    }

    /**
     * Cho người chơi 2 tham gia trận đấu.
     */
    public static void joinMatch(String matchId, String player2Id, String player2DisplayName)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot userDoc = db.collection("users").document(player2Id).get().get();
        long player2Elo = userDoc.exists() ? userDoc.getLong("elo") : 1000;

        Map<String, Object> player2 = new HashMap<>();
        player2.put("uid", player2Id);
        player2.put("displayName", player2DisplayName);
        player2.put("elo", player2Elo);

        Map<String, Object> updates = new HashMap<>();
        updates.put("player2", player2);
        updates.put("status", "IN_PROGRESS");
        updates.put("lastMoveTimestamp", com.google.cloud.Timestamp.now());

        db.collection("matches").document(matchId).update(updates).get();
    }

    /**
     * Lưu nước đi, cập nhật thời gian, và chuyển lượt đi.
     */
    public static void saveMoveAndSwitchTurn(String matchId, DocumentSnapshot matchState, MoveBEAN move)
            throws ExecutionException, InterruptedException {
        WriteBatch batch = db.batch();

        String lastTurn = matchState.getString("currentTurn");
        Timestamp lastMoveTimestamp = matchState.getTimestamp("lastMoveTimestamp");
        long timeElapsed = com.google.cloud.Timestamp.now().toDate().getTime() - lastMoveTimestamp.toDate().getTime();

        String timeLeftField = "Red".equals(lastTurn) ? "player1TimeLeftMs" : "player2TimeLeftMs";
        long oldTimeLeft = matchState.getLong(timeLeftField);
        long newTimeLeft = oldTimeLeft - timeElapsed;

        DocumentReference matchRef = db.collection("matches").document(matchId);
        Map<String, Object> matchUpdates = new HashMap<>();
        matchUpdates.put(timeLeftField, newTimeLeft);
        matchUpdates.put("currentTurn", "Red".equals(lastTurn) ? "Black" : "Red");
        matchUpdates.put("lastMoveTimestamp", com.google.cloud.Timestamp.now());
        batch.update(matchRef, matchUpdates);

        DocumentReference moveRef = matchRef.collection("moves").document();
        batch.set(moveRef, move);

        batch.commit().get();
    }

    /**
     * Tái tạo lại bàn cờ từ lịch sử nước đi để xác thực ở server.
     */
    public static Board reconstructBoardFromMoves(String matchId) throws ExecutionException, InterruptedException {
        Board board = new Board(); // Bắt đầu với bàn cờ mới
        ApiFuture<QuerySnapshot> future = db.collection("matches").document(matchId).collection("moves")
                .orderBy("timestamp").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            board.executeMove(
                    doc.getLong("startX").intValue(),
                    doc.getLong("startY").intValue(),
                    doc.getLong("endX").intValue(),
                    doc.getLong("endY").intValue());
        }
        return board;
    }
}
