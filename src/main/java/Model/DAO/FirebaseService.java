package Model.DAO;

import Model.BEAN.Board;
import Model.BEAN.MatchBEAN;
import Model.BEAN.MoveBEAN;
import Model.BEAN.PieceBEAN;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import java.util.ArrayList;
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
    public static String createNewMatch(String player1Id, String player1DisplayName, long initialTimeMs, boolean isRanked, String sidePreference)
            throws ExecutionException, InterruptedException {
        
        // Bước 1: Yêu cầu Firestore tạo một document mới trong collection 'matches' và lấy ID của nó.
        // Document này vẫn còn trống ở thời điểm hiện tại.
        DocumentReference docRef = db.collection("matches").document();

        // Bước 2: Lấy thông tin ELO của người tạo phòng từ collection 'users'.
        DocumentSnapshot userDoc = db.collection("users").document(player1Id).get().get();
        long player1Elo = userDoc.exists() ? userDoc.getLong("elo") : 1000;
        
        // Tạo một đối tượng Map để lưu thông tin của người chơi 1.
        // Map này sẽ được lưu như một object lồng trong document trận đấu.
        Map<String, Object> player1Info = new HashMap<>();
        player1Info.put("uid", player1Id);
        player1Info.put("displayName", player1DisplayName);
        player1Info.put("elo", player1Elo);

        // Bước 3: Chuẩn bị một Map lớn chứa tất cả dữ liệu cho document trận đấu.
        Map<String, Object> matchData = new HashMap<>();

        // --- CÁC TRƯỜNG THÔNG TIN CƠ BẢN CỦA TRẬN ĐẤU ---
        
        matchData.put("creatorId", player1Id); 
        
        matchData.put("status", "WAITING"); // Trạng thái ban đầu luôn là "Đang chờ".
        matchData.put("currentTurn", "Red"); // Theo luật, quân Đỏ luôn đi trước.
        matchData.put("boardState", boardToMap(new Board())); // Tạo bàn cờ ban đầu.
        matchData.put("startTime", Timestamp.now()); // Thời điểm trận đấu được tạo.
        matchData.put("lastMoveTimestamp", Timestamp.now()); // Khởi tạo mốc thời gian.
        matchData.put("participantIds", java.util.Arrays.asList(player1Id)); // Mảng chứa ID người chơi, dùng cho truy vấn lịch sử.
        matchData.put("boardHistory", new HashMap<String, Long>()); // Map để theo dõi lặp lại nước đi.

        // --- CÁC TRƯỜNG MỚI DỰA TRÊN CÀI ĐẶT CỦA NGƯỜI DÙNG ---
        matchData.put("isRanked", isRanked); // Lưu đây là trận xếp hạng hay không.
        matchData.put("initialTimeMs", initialTimeMs); // Lưu cài đặt thời gian gốc (ví dụ: 600000ms cho 10 phút).
        matchData.put("player1TimeLeftMs", initialTimeMs); // Set đồng hồ đếm ngược cho người chơi 1.
        matchData.put("player2TimeLeftMs", initialTimeMs); // Set đồng hồ đếm ngược cho người chơi 2.
        
        // --- XỬ LÝ LỰA CHỌN MÀU QUÂN ---
        // Logic này quyết định người tạo phòng sẽ được gán vào 'player1' (Đỏ) hay 'player2' (Đen).
        if ("Black".equals(sidePreference)) {
            // Nếu người tạo muốn chơi quân Đen:
            // - Để trống 'player1' (slot cho quân Đỏ).
            // - Gán thông tin người tạo vào 'player2' (slot cho quân Đen).
            matchData.put("player1", null);
            matchData.put("player2", player1Info);
        } else {
            // Nếu người tạo muốn chơi quân Đỏ hoặc Ngẫu nhiên:
            // - Mặc định gán họ vào 'player1' (slot cho quân Đỏ).
            // - Để trống 'player2'.
            // Khi người chơi khác tham gia một phòng "Ngẫu nhiên", logic ở JoinGameServlet có thể quyết định hoán đổi vị trí.
            matchData.put("player1", player1Info);
            matchData.put("player2", null);
        }

        // Bước 4: Ghi toàn bộ dữ liệu trong Map vào document trên Firestore.
        // Lệnh .get() ở cuối để đảm bảo thao tác ghi đã hoàn tất trước khi tiếp tục.
        docRef.set(matchData).get();
        
        // Bước 5: Trả về ID của document vừa tạo để Servlet có thể chuyển hướng người dùng.
        return docRef.getId();
    }
    
    
    public static Map<String, String> boardToMap(Board board) {
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
    /**
     * >>> HÀM ĐÃ ĐƯỢC TỐI ƯU HÓA HOÀN TOÀN <<<
     * Xử lý nước đi, xác thực, kiểm tra chiếu bí, VÀ cập nhật trạng thái bàn cờ.
     * @return String cho biết kết quả: "SUCCESS", "CHECKMATE", hoặc "INVALID_MOVE".
     */
    public static String processMoveAndUpdateBoard(String matchId, int startX, int startY, int endX, int endY) throws Exception {
        DocumentReference matchRef = db.collection("matches").document(matchId);
        DocumentSnapshot matchState = matchRef.get().get();

        if (matchState == null || !"IN_PROGRESS".equals(matchState.getString("status"))) {
            System.out.println("SERVER VALIDATION FAILED: Match not found or not in progress.");
            return "INVALID_MOVE";
        }

        // 1. Tái tạo bàn cờ từ trạng thái trên Firestore
        @SuppressWarnings("unchecked")
        Map<String, String> currentBoardStateMap = (Map<String, String>) matchState.get("boardState");
        Board currentBoard = new Board(currentBoardStateMap);

        // 2. Xác thực nước đi bằng logic của Board
        if (!currentBoard.isMoveValid(startX, startY, endX, endY)) {
            System.out.println("SERVER VALIDATION FAILED: Move is invalid according to game rules.");
            return "INVALID_MOVE"; // Nước đi không hợp lệ, trả về ngay
        }
        Map<String, Long> boardHistory = (Map<String, Long>) matchState.get("boardHistory");
        if (boardHistory == null) {
            boardHistory = new HashMap<>();
        }
        String newBoardKey = generateBoardStateKey(currentBoard);
        // Cập nhật số lần lặp lại
        long repetitionCount = boardHistory.getOrDefault(newBoardKey, 0L) + 1;
        boardHistory.put(newBoardKey, repetitionCount);

        // Kiểm tra hòa do lặp lại
        if (repetitionCount >= 3) {
            System.out.println("!!! DRAW BY 3-FOLD REPETITION !!!");
            endMatchAsDraw(matchId, "DRAW_REPETITION");
            
            // Cập nhật chỉ số hòa
//            Map<String, Object> p1 = (Map<String, Object>) matchState.get("player1");
//            Map<String, Object> p2 = (Map<String, Object>) matchState.get("player2");
            //UserService.updateUserStatsAfterMatch((String)p1.get("uid"), (String)p2.get("uid"), true);
            
            // Trả về kết quả đặc biệt để GameEndpoint có thể thông báo
            return "DRAW_REPETITION";
        }

        // 3. Nước đi hợp lệ. Lấy thông tin cần thiết TRƯỚC KHI thực hiện.
        String currentTurnColor = matchState.getString("currentTurn");
        String opponentColor = "Red".equals(currentTurnColor) ? "Black" : "Red";
        PieceBEAN pieceMoved = currentBoard.getPieceAt(startX, startY);

        // 4. >>> THỰC HIỆN NƯỚC ĐI TRÊN ĐỐI TƯỢNG BOARD TRONG BỘ NHỚ <<<
        currentBoard.executeMove(startX, startY, endX, endY);
        
        boolean opponentHasMoves = currentBoard.hasLegalMoves(opponentColor);
        boolean isGameOver = !opponentHasMoves;
        String winReason = null;
        
        if (isGameOver) {
            // Nếu hết nước đi:
            // - Nếu đang bị chiếu -> Là Chiếu bí (CHECKMATE)
            // - Nếu không bị chiếu -> Là Vây khốn (STALEMATE)
            if (currentBoard.isKingInCheck(opponentColor)) {
                winReason = "CHECKMATE";
                System.out.println("!!! CHECKMATE DETECTED !!!");
            } else {
                winReason = "STALEMATE"; // Hết nước đi
                System.out.println("!!! STALEMATE DETECTED !!!");
            }
        }

        // 5. >>> KIỂM TRA CHIẾU BÍ NGAY TRÊN BOARD TRONG BỘ NHỚ <<<
        //boolean isCheckmate = currentBoard.isCheckmate(opponentColor);

        // 6. Chuẩn bị dữ liệu để cập nhật lên Firestore
        Map<String, String> newBoardStateMap = boardToMap(currentBoard);
        Map<String, Object> matchUpdates = new HashMap<>();
        matchUpdates.put("boardState", newBoardStateMap);
        matchUpdates.put("currentTurn", opponentColor); // Luôn đổi lượt
        matchUpdates.put("lastMoveTimestamp", Timestamp.now());

        // --- Cập nhật thời gian còn lại (logic cũ vẫn đúng) ---
        Timestamp lastMoveTimestamp = matchState.getTimestamp("lastMoveTimestamp");
        long timeElapsedMs = Timestamp.now().toDate().getTime() - lastMoveTimestamp.toDate().getTime();
        String timeLeftField = "Red".equals(currentTurnColor) ? "player1TimeLeftMs" : "player2TimeLeftMs";
        long oldTimeLeftMs = matchState.getLong(timeLeftField);
        long newTimeLeftMs = Math.max(0, oldTimeLeftMs - timeElapsedMs);
        matchUpdates.put(timeLeftField, newTimeLeftMs);
        matchUpdates.put("boardHistory", boardHistory); // <-- Quan trọng: Lưu lại lịch sử
        
        
        
        // --- Nếu là chiếu bí, cập nhật thêm trạng thái kết thúc trận đấu ---
//        if (isCheckmate) {
//            System.out.println("!!! CHECKMATE DETECTED !!!");
//            Map<String, String> winnerInfo = (Map<String, String>) matchState.get(currentTurnColor.equals("Red") ? "player1" : "player2");
//            
//            matchUpdates.put("status", "COMPLETED");
//            matchUpdates.put("winReason", "CHECKMATE");
//            matchUpdates.put("winnerId", winnerInfo.get("uid"));
//            
//            // Ghi chú: hàm endMatch() cũ không còn cần thiết cho trường hợp chiếu bí nữa
//        }
        if (isGameOver) {
            Map<String, String> winnerInfo = (Map<String, String>) matchState.get(currentTurnColor.equals("Red") ? "player1" : "player2");
            matchUpdates.put("status", "COMPLETED");
            matchUpdates.put("winReason", winReason); // CHECKMATE hoặc STALEMATE
            matchUpdates.put("winnerId", winnerInfo.get("uid"));
        }

        // 7. Ghi tất cả thay đổi lên Firestore trong một batch
        WriteBatch batch = db.batch();
        
        // Thao tác 1: Cập nhật document trận đấu
        batch.update(matchRef, matchUpdates);

        // Thao tác 2: Lưu lại lịch sử nước đi
        MoveBEAN moveRecord = new MoveBEAN(
            currentTurnColor, startX, startY, endX, endY,
            pieceMoved.getClass().getSimpleName(), Timestamp.now()
        );
        DocumentReference moveRef = matchRef.collection("moves").document();
        batch.set(moveRef, moveRecord);

        // 8. Commit và trả về kết quả
        batch.commit().get();
        
        if (isGameOver) {
            return winReason; // Trả về "CHECKMATE" hoặc "STALEMATE"
        } else {
            return "SUCCESS";
        }
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
    public static void joinMatch(String matchId, String joiningPlayerId, String joiningPlayerDisplayName) 
            throws ExecutionException, InterruptedException {
        
        // Bước 1: Lấy thông tin ELO của người chơi đang tham gia
        DocumentSnapshot userDoc = db.collection("users").document(joiningPlayerId).get().get();
        long joiningPlayerElo = userDoc.exists() ? userDoc.getLong("elo") : 1000;
        
        // Tạo Map chứa thông tin của người chơi
        Map<String, Object> joiningPlayerInfo = new HashMap<>();
        joiningPlayerInfo.put("uid", joiningPlayerId);
        joiningPlayerInfo.put("displayName", joiningPlayerDisplayName);
        joiningPlayerInfo.put("elo", joiningPlayerElo);
        
        // Bước 2: Lấy trạng thái hiện tại của trận đấu để xác định vị trí trống
        DocumentReference matchRef = db.collection("matches").document(matchId);
        DocumentSnapshot matchDoc = matchRef.get().get();

        Map<String, Object> updates = new HashMap<>();
        
        // --- LOGIC QUAN TRỌNG ---
        // Kiểm tra xem player1 có trống không. Nếu có, điền vào đó.
        if (matchDoc.get("player1") == null) {
            updates.put("player1", joiningPlayerInfo);
        } 
        // Nếu không, kiểm tra player2. Nếu có, điền vào đó.
        else if (matchDoc.get("player2") == null) {
            updates.put("player2", joiningPlayerInfo);
        } else {
            // Trường hợp phòng đã đầy, không làm gì cả (Servlet đã kiểm tra nhưng đây là lớp phòng thủ)
            return; 
        }
        
        // Bước 3: Cập nhật các trường thông tin khác
        updates.put("status", "IN_PROGRESS"); // Trận đấu bắt đầu!
        updates.put("lastMoveTimestamp", com.google.cloud.Timestamp.now()); // Reset đồng hồ cho nước đi đầu tiên
        updates.put("participantIds", FieldValue.arrayUnion(joiningPlayerId)); // Thêm ID vào mảng người tham gia
        
        // Ghi tất cả cập nhật lên Firestore
        matchRef.update(updates).get();
    }
    
    public static List<MatchBEAN> getMatchHistory(String userId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = db.collection("matches")
                .whereArrayContains("participantIds", userId)
                .whereEqualTo("status", "COMPLETED")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(50)
                .get();
        
        List<MatchBEAN> matchHistory = new java.util.ArrayList<>();
        for (DocumentSnapshot doc : future.get().getDocuments()) {
            // Firestore SDK có thể tự động chuyển đổi DocumentSnapshot thành POJO (Plain Old Java Object)
            MatchBEAN match = doc.toObject(MatchBEAN.class);
            match.setMatchId(doc.getId()); // Đừng quên set ID của trận đấu
            matchHistory.add(match);
        }
        
        return matchHistory;
    }
    
    public static List<MoveBEAN> getMovesForMatch(String matchId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = db.collection("matches").document(matchId).collection("moves")
                .orderBy("timestamp", Query.Direction.ASCENDING) // Quan trọng: Sắp xếp các nước đi theo đúng thứ tự thời gian
                .get();

        List<MoveBEAN> moves = new java.util.ArrayList<>();
        for (DocumentSnapshot doc : future.get().getDocuments()) {
            moves.add(doc.toObject(MoveBEAN.class));
        }
        return moves;
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
    public static List<Map<String, Object>> getWaitingMatches() throws ExecutionException, InterruptedException {
        List<Map<String, Object>> waitingMatches = new ArrayList<>();
        
        // Tạo truy vấn đến collection 'matches' với điều kiện status == "WAITING"
        ApiFuture<QuerySnapshot> future = db.collection("matches").whereEqualTo("status", "WAITING").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        // Lặp qua từng document kết quả
        for (QueryDocumentSnapshot document : documents) {
            Map<String, Object> matchData = document.getData();
            // QUAN TRỌNG: Thêm ID của document (chính là gameId) vào trong Map
            // để frontend có thể sử dụng nó cho nút "Join".
            matchData.put("matchId", document.getId());
            waitingMatches.add(matchData);
        }
        
        return waitingMatches;
    }
    public static void endMatchAsDraw(String matchId, String reason) 
            throws ExecutionException, InterruptedException {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("winnerId", null); // Không có người thắng
        updates.put("winReason", reason); // e.g., "DRAW_AGREEMENT"
        
        db.collection("matches").document(matchId).update(updates).get();
    }
    private static String generateBoardStateKey(Board board) {
        StringBuilder sb = new StringBuilder();
        // Sắp xếp để đảm bảo key luôn nhất quán
        java.util.TreeMap<String, String> sortedBoard = new java.util.TreeMap<>(boardToMap(board));
        for (Map.Entry<String, String> entry : sortedBoard.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }
    
    public static List<int[]> getValidMovesForPiece(String matchId, int pieceX, int pieceY) throws Exception {
        DocumentSnapshot matchState = getMatch(matchId);
        if (matchState == null) {
            throw new Exception("Match not found.");
        }

        // Tái tạo lại bàn cờ từ trạng thái hiện tại
        Map<String, String> currentBoardStateMap = (Map<String, String>) matchState.get("boardState");
        Board currentBoard = new Board(currentBoardStateMap);

        List<int[]> validMoves = new ArrayList<>();
        
        // Duyệt qua tất cả các ô trên bàn cờ
        for (int endY = 0; endY < 10; endY++) {
            for (int endX = 0; endX < 9; endX++) {
                // Sử dụng hàm isMoveValid đã có sẵn để kiểm tra
                if (currentBoard.isMoveValid(pieceX, pieceY, endX, endY)) {
                    validMoves.add(new int[]{endX, endY});
                }
            }
        }
        return validMoves;
    }
    
    public static void deleteWaitingMatch(String matchId, String requesterId) 
            throws ExecutionException, InterruptedException {
                
        DocumentReference matchRef = db.collection("matches").document(matchId);
        DocumentSnapshot matchDoc = matchRef.get().get();

        if (!matchDoc.exists()) {
            System.out.println("Không thể xóa: Trận đấu " + matchId + " không tồn tại.");
            return;
        }

        // Chỉ xóa nếu trận đấu vẫn đang ở trạng thái "WAITING"
        if (!"WAITING".equals(matchDoc.getString("status"))) {
            System.out.println("Không thể xóa: Trận đấu " + matchId + " không còn ở trạng thái chờ.");
            return;
        }

        // Lấy thông tin người tạo phòng (có thể là player1 hoặc player2)
        Map<String, Object> creatorInfo = null;
        if (matchDoc.get("player1") != null) {
            creatorInfo = (Map<String, Object>) matchDoc.get("player1");
        } else if (matchDoc.get("player2") != null) {
            creatorInfo = (Map<String, Object>) matchDoc.get("player2");
        }

        if (creatorInfo == null) {
            System.out.println("Không thể xóa: Không tìm thấy thông tin người tạo phòng cho trận " + matchId);
            return;
        }

        String creatorId = (String) creatorInfo.get("uid");

        // KIỂM TRA BẢO MẬT QUAN TRỌNG NHẤT:
        // Người yêu cầu xóa có phải là người đã tạo phòng không?
        if (requesterId.equals(creatorId)) {
            // Nếu đúng, tiến hành xóa document
            matchRef.delete().get();
            System.out.println("Thành công: Đã xóa phòng chờ " + matchId + " do người tạo phòng thoát.");
        } else {
            System.out.println("Không thể xóa: User " + requesterId + " không có quyền xóa phòng " + matchId + " của user " + creatorId);
        }
    }
}
