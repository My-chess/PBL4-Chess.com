package Model.DAO;

import Model.BEAN.Board;
import Model.BEAN.MoveBEAN;
import Model.BEAN.PieceBEAN;

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
        
        // Tạo bàn cờ ban đầu và chuyển nó thành Map
        Board initialBoard = new Board();
        Map<String, String> initialBoardState = boardToMap(initialBoard);

        // ... (code lấy elo người chơi giữ nguyên) ...

        Map<String, Object> match = new HashMap<>();
        // ... (các trường thông tin player, status, v.v. giữ nguyên) ...
        match.put("status", "WAITING");
        match.put("currentTurn", "Red");
        // >>> THÊM MỚI: Lưu trạng thái bàn cờ vào document <<<
        match.put("boardState", initialBoardState); 

        docRef.set(match).get();
        return docRef.getId();
    }
    
    private static Map<String, String> boardToMap(Board board) {
        Map<String, String> boardState = new HashMap<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN piece = board.getPieceAt(x, y);
                if (piece != null) {
                    String key = y + "," + x;
                    String value = piece.getClass().getSimpleName() + "_" + piece.getColor();
                    boardState.put(key, value);
                }
            }
        }
        return boardState;
    }
    public static boolean processMoveAndUpdateBoard(String matchId, int startX, int startY, int endX, int endY) throws Exception {
        DocumentSnapshot matchState = getMatch(matchId);
        if (matchState == null) return false;

        // 1. Lấy trạng thái bàn cờ hiện tại từ Firestore
        Map<String, String> currentBoardStateMap = (Map<String, String>) matchState.get("boardState");
        
        // 2. Tái tạo đối tượng Board từ trạng thái đó
        Board currentBoard = new Board(currentBoardStateMap);
        
        // 3. Kiểm tra nước đi có hợp lệ không
        if (!currentBoard.isMoveValid(startX, startY, endX, endY)) {
            System.out.println("SERVER VALIDATION FAILED: Move is invalid.");
            return false; // Trả về false nếu không hợp lệ
        }
        
        // 4. Nếu hợp lệ, thực hiện nước đi trên đối tượng Board
        PieceBEAN pieceMoved = currentBoard.getPieceAt(startX, startY);
        currentBoard.executeMove(startX, startY, endX, endY);
        
        // 5. Chuyển đổi bàn cờ sau khi đi thành Map để lưu lại
        Map<String, String> newBoardStateMap = boardToMap(currentBoard);
        
        // 6. Chuẩn bị dữ liệu để cập nhật và lưu nước đi vào "moves" subcollection
        WriteBatch batch = db.batch();
        DocumentReference matchRef = db.collection("matches").document(matchId);
        
        // Cập nhật các thông tin của trận đấu
        String lastTurn = matchState.getString("currentTurn");
        Map<String, Object> matchUpdates = new HashMap<>();
        matchUpdates.put("boardState", newBoardStateMap); // <-- Cập nhật trạng thái bàn cờ mới
        matchUpdates.put("currentTurn", "Red".equals(lastTurn) ? "Black" : "Red");
        matchUpdates.put("lastMoveTimestamp", Timestamp.now());
        // ... (code cập nhật thời gian còn lại giữ nguyên) ...
        batch.update(matchRef, matchUpdates);
        
        // Lưu nước đi này vào subcollection để làm lịch sử
        MoveBEAN move = new MoveBEAN(lastTurn, startX, startY, endX, endY, pieceMoved.getClass().getSimpleName(), Timestamp.now());
        DocumentReference moveRef = matchRef.collection("moves").document();
        batch.set(moveRef, move);

        // 7. Thực thi batch write
        batch.commit().get();
        return true;
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
    public static void endMatch(String matchId, String winnerId, String reason) 
            throws ExecutionException, InterruptedException {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("winnerId", winnerId);
        updates.put("winReason", reason); // "CHECKMATE" hoặc "TIMEOUT"
        
        db.collection("matches").document(matchId).update(updates).get();
    }
}
