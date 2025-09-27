package Controller; // Giữ nguyên package của bạn

import Model.BEAN.Board;
import Model.BEAN.MoveBEAN;
import Model.DAO.FirebaseService;
import Model.DAO.UserService; // Import UserService để cập nhật ELO
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp GameEndpoint là trái tim của giao tiếp thời gian thực (real-time).
 * Nó xử lý tất cả các kết nối WebSocket từ client, nhận các hành động
 * như di chuyển quân cờ, xử lý hết giờ, và thông báo trạng thái game cho người chơi.
 */
@ServerEndpoint("/game/{gameId}")
public class GameEndpoint {
    // Sử dụng Gson để chuyển đổi giữa đối tượng Java và chuỗi JSON.
    private static final Gson gson = new Gson();
    // Một Map để lưu trữ tất cả các session đang kết nối vào một game cụ thể.
    // Key: gameId, Value: Map<sessionId, Session>
    private static final Map<String, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();

    /**
     * Được gọi khi một client mới kết nối tới WebSocket.
     * @param session Đối tượng session của client vừa kết nối.
     * @param gameId ID của ván cờ mà client tham gia.
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("gameId") String gameId) {
        System.out.println("--- WebSocket Connection Opened ---");
        System.out.println("GameID: " + gameId + ", SessionID: " + session.getId());

        // Thêm session của client vào danh sách các session của ván cờ này.
        // Điều này cho phép server gửi tin nhắn đến tất cả người chơi trong cùng một ván.
        gameSessions.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
    }

    /**
     * >>> FILE ĐƯỢC CẤU TRÚC LẠI HOÀN TOÀN <<<
     * Được gọi khi server nhận một tin nhắn từ client.
     * @param message Tin nhắn dạng chuỗi JSON từ client.
     * @param session Session của client gửi tin nhắn.
     * @param gameId ID của ván cờ.
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("gameId") String gameId) throws IOException {
        System.out.println("\n--- Message Received on Server ---");
        System.out.println("From GameID: " + gameId);
        System.out.println("Raw Message: " + message);

        try {
            // Phân tích tin nhắn JSON thành một Map để xác định loại hành động.
            Map<String, Object> messageData = gson.fromJson(message, Map.class);
            String messageType = (String) messageData.get("type");

            // Xử lý tùy theo loại tin nhắn
            if ("move".equals(messageType)) {
                handleMoveMessage(messageData, session, gameId);
            } else if ("timeout".equals(messageType)) {
                handleTimeoutMessage(session, gameId);
            } else {
                sendError(session, "Loại tin nhắn không hợp lệ.");
            }

        } catch (JsonSyntaxException e) {
            System.out.println("!!! Lỗi phân tích JSON: " + message);
            sendError(session, "Định dạng tin nhắn không hợp lệ.");
        } catch (Exception e) {
            System.out.println("!!! AN EXCEPTION OCCURRED ON SERVER !!!");
            e.printStackTrace();
            sendError(session, "Lỗi phía server: " + e.getMessage());
        }
    }

    /**
     * Xử lý tin nhắn di chuyển quân cờ.
     */
    private void handleMoveMessage(Map<String, Object> messageData, Session session, String gameId) throws Exception {
        // Lấy dữ liệu nước đi từ tin nhắn
        Map<String, Double> movePayload = (Map<String, Double>) messageData.get("data");
        int startX = movePayload.get("startX").intValue();
        int startY = movePayload.get("startY").intValue();
        int endX = movePayload.get("endX").intValue();
        int endY = movePayload.get("endY").intValue();
        System.out.println("Parsed move: from [" + startX + "," + startY + "] to [" + endX + "," + endY + "]");

        // 1. Lấy trạng thái trận đấu hiện tại từ Firestore.
        DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
        if (matchState == null || !"IN_PROGRESS".equals(matchState.getString("status"))) {
            sendError(session, "Trận đấu không tồn tại hoặc đã kết thúc.");
            return;
        }

        // 2. TODO: Bổ sung logic kiểm tra xem có đúng lượt của người chơi này không
        // (Phần này bạn có thể tự hoàn thiện bằng cách so sánh ID người chơi trong session và trong matchState)

        // 3. Tái tạo lại bàn cờ từ lịch sử các nước đi.
        System.out.println("Reconstructing board from moves...");
        Board currentBoard = FirebaseService.reconstructBoardFromMoves(gameId);
        
        // 4. Sử dụng hàm isMoveValid đã được hoàn thiện để kiểm tra nước đi.
        System.out.println("Validating move...");
        if (currentBoard.isMoveValid(startX, startY, endX, endY)) {
            System.out.println("Validation SUCCESS: Move is valid.");
            
            String currentTurnColor = matchState.getString("currentTurn");
            MoveBEAN move = new MoveBEAN(
                currentTurnColor,
                startX, startY, endX, endY,
                currentBoard.getPieceAt(startX, startY).getClass().getSimpleName(),
                Timestamp.now()
            );
            
            // 5. Lưu nước đi vào Firestore và chuyển lượt.
            System.out.println("Saving move to Firestore...");
            FirebaseService.saveMoveAndSwitchTurn(gameId, matchState, move);
            System.out.println("Move saved successfully.");
            
            // Thông báo cho client là nước đi đã được chấp nhận.
            session.getBasicRemote().sendText("{\"type\":\"MOVE_ACCEPTED\"}");

            // 6. >>> LOGIC MỚI: KIỂM TRA CHIẾU BÍ SAU KHI ĐI <<<
            System.out.println("Checking for checkmate...");
            Board boardAfterMove = FirebaseService.reconstructBoardFromMoves(gameId); // Tải lại bàn cờ sau nước đi mới
            String nextTurnColor = "Red".equals(currentTurnColor) ? "Black" : "Red";
            
            if (boardAfterMove.isCheckmate(nextTurnColor)) {
                System.out.println("!!! CHECKMATE !!! " + currentTurnColor + " wins!");
                // Lấy ID người chơi từ matchState
                String winnerId = ((Map<String, String>)matchState.get(currentTurnColor.equals("Red") ? "player1" : "player2")).get("uid");
                String loserId = ((Map<String, String>)matchState.get(nextTurnColor.equals("Red") ? "player1" : "player2")).get("uid");
                endGame(gameId, winnerId, loserId, currentTurnColor, "CHECKMATE");
            }

        } else {
            System.out.println("Validation FAILED: Move is NOT valid according to game rules.");
            sendError(session, "Nước đi không hợp lệ!");
        }
    }
    
    /**
     * >>> HÀM MỚI: Xử lý tin nhắn khi một người chơi hết giờ <<<
     */
    private void handleTimeoutMessage(Session session, String gameId) throws Exception {
        System.out.println("Processing timeout for game: " + gameId);
        DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
        
        if (matchState == null || !"IN_PROGRESS".equals(matchState.getString("status"))) {
            return; // Trận đấu đã kết thúc, không xử lý nữa
        }

        // Xác định người thua (người đang trong lượt) và người thắng
        String loserColor = matchState.getString("currentTurn");
        String winnerColor = "Red".equals(loserColor) ? "Black" : "Red";

        String loserId = ((Map<String, String>)matchState.get(loserColor.equals("Red") ? "player1" : "player2")).get("uid");
        String winnerId = ((Map<String, String>)matchState.get(winnerColor.equals("Red") ? "player1" : "player2")).get("uid");

        System.out.println("!!! TIMEOUT !!! " + winnerColor + " wins!");
        endGame(gameId, winnerId, loserId, winnerColor, "TIMEOUT");
    }

    /**
     * >>> HÀM MỚI: Logic kết thúc ván cờ <<<
     * Cập nhật DB, cập nhật ELO và thông báo cho tất cả người chơi.
     */
    private void endGame(String gameId, String winnerId, String loserId, String winnerColor, String reason) {
        try {
            // 1. Cập nhật trạng thái trận đấu trên Firestore (có thể tạo hàm mới trong FirebaseService)
            FirebaseService.endMatch(gameId, winnerId, reason); // Giả sử có hàm này
            
            // 2. Cập nhật chỉ số (win/lose, ELO) cho người chơi
            UserService.updateUserStatsAfterMatch(winnerId, loserId, false);

            // 3. Gửi thông báo kết thúc game đến tất cả client trong phòng
            Map<String, Object> gameOverMessage = new ConcurrentHashMap<>();
            gameOverMessage.put("type", "GAME_OVER");
            gameOverMessage.put("winnerColor", winnerColor);
            gameOverMessage.put("reason", reason); // "CHECKMATE" hoặc "TIMEOUT"
            
            broadcast(gameId, gson.toJson(gameOverMessage));

        } catch (Exception e) {
            System.err.println("Error ending game: " + gameId);
            e.printStackTrace();
        }
    }

    /**
     * Được gọi khi một client đóng kết nối WebSocket.
     */
    @OnClose
    public void onClose(Session session, @PathParam("gameId") String gameId) {
        System.out.println("--- WebSocket Connection Closed for GameID: " + gameId + " ---");
        // Xóa session khỏi danh sách
        Map<String, Session> sessions = gameSessions.get(gameId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                gameSessions.remove(gameId);
            }
        }
    }

    /**
     * Được gọi khi có lỗi xảy ra trong quá trình giao tiếp WebSocket.
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("!!! WEBSOCKET ERROR !!!");
        throwable.printStackTrace();
    }
    
    /**
     * Gửi tin nhắn đến tất cả các client đang kết nối vào cùng một ván cờ.
     * @param gameId ID của ván cờ.
     * @param message Tin nhắn cần gửi.
     */
    private void broadcast(String gameId, String message) {
        Map<String, Session> sessions = gameSessions.get(gameId);
        if (sessions != null) {
            sessions.values().forEach(session -> {
                synchronized (session) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Gửi một tin nhắn lỗi tới một client cụ thể.
     * @param session Session của client.
     * @param message Nội dung lỗi.
     */
    private void sendError(Session session, String message) throws IOException {
        session.getBasicRemote().sendText(String.format("{\"type\":\"ERROR\", \"message\":\"%s\"}", message));
    }
}