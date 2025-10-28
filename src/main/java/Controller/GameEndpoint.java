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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Lớp GameEndpoint là trái tim của giao tiếp thời gian thực (real-time).
 * Nó xử lý tất cả các kết nối WebSocket từ client, nhận các hành động
 * như di chuyển quân cờ, xử lý hết giờ, và thông báo trạng thái game cho người chơi.
 */
@ServerEndpoint("/game/{gameId}/{userId}")
public class GameEndpoint {
    // Sử dụng Gson để chuyển đổi giữa đối tượng Java và chuỗi JSON.
    private static final Gson gson = new Gson();
    // Một Map để lưu trữ tất cả các session đang kết nối vào một game cụ thể.
    // Key: gameId, Value: Map<sessionId, Session>
    private static final Map<String, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();
    
    private static final ScheduledExecutorService afkScheduler = Executors.newScheduledThreadPool(1);
    // Map để lưu trữ tác vụ kiểm tra AFK cho mỗi ván cờ
    private static final Map<String, ScheduledFuture<?>> afkTasks = new ConcurrentHashMap<>();

    /**
     * Được gọi khi một client mới kết nối tới WebSocket.
     * @param session Đối tượng session của client vừa kết nối.
     * @param gameId ID của ván cờ mà client tham gia.
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("gameId") String gameId, @PathParam("userId") String userId) {
        System.out.println("--- WebSocket Connection Opened ---");
        System.out.println("GameID: " + gameId + ", SessionID: " + session.getId() + ", UserID: " + userId);
        
        // Lấy map các session của game này
        Map<String, Session> sessionsInGame = gameSessions.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        
        // Nếu đã có session cũ của user này, đóng nó lại
        Session oldSession = sessionsInGame.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                System.out.println("Closing old session for user: " + userId);
                oldSession.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Thêm session MỚI vào, ghi đè lên session cũ (nếu có)
        // Bây giờ, userId là key chính
        sessionsInGame.put(userId, session);
        
        // Lưu userId vào session để dùng ở onClose
        session.getUserProperties().put("userId", userId);
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
            switch(messageType) {
            case "move": handleMoveMessage(messageData, session, gameId); break;
            case "timeout": handleTimeoutMessage(session, gameId); break;
            case "resign": handleResignMessage(session, gameId); break;
            case "offer_draw": handleOfferDraw(session, gameId); break;
            case "accept_draw": handleAcceptDraw(session, gameId); break;
            case "decline_draw": handleDeclineDraw(session, gameId); break;
            default: sendError(session, "Loại tin nhắn không hợp lệ."); break;
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
 // Dán đoạn mã này vào bên trong lớp `GameEndpoint.java` của bạn,
 // thay thế cho hàm handleMoveMessage cũ.

 /**
  * Xử lý tin nhắn yêu cầu di chuyển quân cờ từ client theo kiến trúc mới.
  *
  * @param messageData Dữ liệu tin nhắn đã được parse từ JSON.
  * @param session     Session của client gửi yêu cầu, để có thể gửi lại thông báo lỗi.
  * @param gameId      ID của ván cờ đang diễn ra.
  * @throws Exception  Ném ra lỗi nếu có vấn đề trong quá trình xử lý.
  */
    private void handleMoveMessage(Map<String, Object> messageData, Session session, String gameId) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Double> movePayload = (Map<String, Double>) messageData.get("data");
        int startX = movePayload.get("startX").intValue();
        int startY = movePayload.get("startY").intValue();
        int endX = movePayload.get("endX").intValue();
        int endY = movePayload.get("endY").intValue();

        System.out.println("Processing move for GameID " + gameId + ": from [" + startX + "," + startY + "] to [" + endX + "," + endY + "]");

        // Bước 1: Gọi hàm service đã được tối ưu
        String moveResult = FirebaseService.processMoveAndUpdateBoard(gameId, startX, startY, endX, endY);
        
        // Bước 2: Xử lý kết quả trả về
        switch (moveResult) {
            case "SUCCESS":
                // Nước đi hợp lệ, trận đấu tiếp tục.
                // KHÔNG CẦN LÀM GÌ CẢ. Client sẽ tự cập nhật giao diện qua onSnapshot.
            	scheduleAfkCheck(gameId);
                System.out.println("Success: Move was valid. Board updated on Firestore.");
                break;
                
            case "CHECKMATE":
                // Nước đi dẫn đến chiếu bí. Trạng thái game trên DB đã là "COMPLETED".
                // Bây giờ ta cần cập nhật ELO và thông báo cho client.
                System.out.println("Success: CHECKMATE! Game is over.");
                
                // Lấy lại trạng thái trận đấu lần cuối để biết ai là người chơi
                DocumentSnapshot finalMatchState = FirebaseService.getMatch(gameId);
                if (finalMatchState == null) return;
                
                // Người vừa đi là người thắng cuộc. Lượt đi lúc này đã được đổi sang cho đối thủ.
                // Vậy nên người thắng là người KHÔNG có lượt đi hiện tại.
                String loserColor = finalMatchState.getString("currentTurn");
                String winnerColor = "Red".equals(loserColor) ? "Black" : "Red";
                
                Map<String, String> players = getWinnerAndLoserIds(finalMatchState, winnerColor);
                if (players != null) {
                    // Gọi hàm kết thúc game để cập nhật ELO và broadcast
                    endGame(gameId, players.get("winnerId"), players.get("loserId"), winnerColor, "CHECKMATE");
                }
                break;
            case "DRAW_REPETITION":
                System.out.println("Success: DRAW BY REPETITION! Game is over.");
                endGameAsDraw(gameId, "DRAW_REPETITION");
                break;
                
            case "INVALID_MOVE":
                // Nước đi không hợp lệ. Gửi lỗi về cho client.
                System.out.println("Failure: Move was invalid. Sending error to client.");
                sendError(session, "Nước đi không hợp lệ!");
                break;
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

        // Người thua là người đang có lượt đi
        String loserColor = matchState.getString("currentTurn");
        String winnerColor = "Red".equals(loserColor) ? "Black" : "Red";

        // Dùng hàm trợ giúp để lấy ID
        Map<String, String> players = getWinnerAndLoserIds(matchState, winnerColor);
        
        if (players != null) {
            System.out.println("!!! TIMEOUT !!! " + winnerColor + " wins!");
            endGame(gameId, players.get("winnerId"), players.get("loserId"), winnerColor, "TIMEOUT");
        }
    }
 // Thêm các hàm xử lý mới này vào trong lớp GameEndpoint
    private void sendToOnePlayer(String gameId, String targetUserId, String message) {
        Map<String, Session> sessionsInGame = gameSessions.get(gameId);
        if (sessionsInGame != null) {
            // Lấy session trực tiếp bằng userId, không cần duyệt
            Session targetSession = sessionsInGame.get(targetUserId);
            if (targetSession != null && targetSession.isOpen()) {
                try {
                    System.out.println("[SERVER LOG] Sending private message to user " + targetUserId);
                    targetSession.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleOfferDraw(Session session, String gameId) throws Exception {
        String offeringUserId = (String) session.getUserProperties().get("userId");
        DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
        Map<String, Object> p1 = (Map<String, Object>) matchState.get("player1");
        Map<String, Object> p2 = (Map<String, Object>) matchState.get("player2");

        String opponentId = offeringUserId.equals(p1.get("uid")) ? (String) p2.get("uid") : (String) p1.get("uid");

        // Gửi tin nhắn tới đối thủ
        sendToOnePlayer(gameId, opponentId, "{\"type\":\"DRAW_OFFER_RECEIVED\"}");
    }

    private void handleAcceptDraw(Session session, String gameId) {
        System.out.println("[SERVER LOG] Handling draw acceptance for game: " + gameId);
        try {
            // Bước 1: Cập nhật trạng thái trận đấu thành "COMPLETED" với lý do "DRAW_AGREEMENT".
            FirebaseService.endMatchAsDraw(gameId, "DRAW_AGREEMENT");

            // Bước 2: Gọi hàm điều phối để xử lý các tác vụ còn lại (cập nhật ELO, gửi broadcast).
            endGameAsDraw(gameId, "DRAW_AGREEMENT");

        } catch (Exception e) {
            System.err.println("[SERVER LOG] CRITICAL ERROR in handleAcceptDraw:");
            e.printStackTrace();
        }
    }

    private void handleDeclineDraw(Session session, String gameId) throws Exception {
        String decliningUserId = (String) session.getUserProperties().get("userId");
        DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
        Map<String, Object> p1 = (Map<String, Object>) matchState.get("player1");
        Map<String, Object> p2 = (Map<String, Object>) matchState.get("player2");
        
        String originalOffererId = decliningUserId.equals(p1.get("uid")) ? (String) p2.get("uid") : (String) p1.get("uid");
        
        // Gửi tin nhắn từ chối về cho người đã mời hòa
        sendToOnePlayer(gameId, originalOffererId, "{\"type\":\"DRAW_OFFER_DECLINED\"}");
    }

    /**
     * >>> HÀM MỚI: Logic kết thúc ván cờ <<<
     * Cập nhật DB, cập nhật ELO và thông báo cho tất cả người chơi.
     */
    private void endGame(String gameId, String winnerId, String loserId, String winnerColor, String reason) {
    	cancelAfkCheck(gameId);
        try {
            // 1. Cập nhật trạng thái trận đấu trên Firestore
            FirebaseService.endMatch(gameId, winnerId, reason);
            
            // 2. Cập nhật chỉ số (win/lose, ELO) cho người chơi
            // SỬA LẠI Ở ĐÂY: Truyền `false` cho tham số isDraw
            // và truyền winnerId, loserId theo đúng thứ tự
            UserService.updateUserStatsAfterMatch(gameId, winnerId, loserId, false);

            // 3. Gửi thông báo kết thúc game đến tất cả client
            Map<String, Object> gameOverMessage = new ConcurrentHashMap<>();
            gameOverMessage.put("type", "GAME_OVER");
            gameOverMessage.put("winnerColor", winnerColor);
            gameOverMessage.put("reason", reason);
            
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
        System.out.println("--- WebSocket Connection Closed for SessionID: " + session.getId() + " ---");
        String disconnectedUserId = (String) session.getUserProperties().get("userId");
        
        Map<String, Session> currentsessionsInGame = gameSessions.get(gameId);
        if (currentsessionsInGame != null && disconnectedUserId != null) {
            // Chỉ xóa session khỏi map nếu nó là session hiện tại
            // Tránh trường hợp một session mới đã ghi đè lên nó
        	currentsessionsInGame.remove(disconnectedUserId, session);
            
            if (currentsessionsInGame.isEmpty()) {
                gameSessions.remove(gameId);
            }
        }

        // >>> LOGIC MỚI: Xử lý bỏ cuộc <<<
        if (disconnectedUserId != null) {
            // Lên lịch một tác vụ xử thua sau 30 giây
            System.out.println("[DISCONNECT] Scheduling forfeit check for user " + disconnectedUserId + " in 30 seconds.");
            ScheduledFuture<?> task = afkScheduler.schedule(() -> {
                try {
                    // Kiểm tra xem người dùng đã kết nối lại chưa
                    Map<String, Session> sessionsInGame = gameSessions.get(gameId);
                    if (sessionsInGame != null && sessionsInGame.containsKey(disconnectedUserId)) {
                        System.out.println("[DISCONNECT] User " + disconnectedUserId + " has reconnected. Forfeit canceled.");
                        return; // Người dùng đã kết nối lại, không làm gì cả
                    }

                    // Nếu sau 30 giây mà người dùng vẫn chưa kết nối lại, xử thua
                    DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
                    if (matchState != null && "IN_PROGRESS".equals(matchState.getString("status"))) {
                        // ... (logic xác định người thắng/thua như cũ) ...
                        Map<String, Object> p1 = (Map<String, Object>) matchState.get("player1");
                        Map<String, Object> p2 = (Map<String, Object>) matchState.get("player2");
                        // ...
                        String winnerId = null;
                        String loserId = null;
                        String winnerColor = null;

                        if (p1 != null && disconnectedUserId.equals(p1.get("uid"))) {
                            loserId = (String) p1.get("uid");
                            if (p2 != null) {
                                winnerId = (String) p2.get("uid");
                                winnerColor = "Black";
                            }
                        } else if (p2 != null && disconnectedUserId.equals(p2.get("uid"))) {
                            loserId = (String) p2.get("uid");
                            if (p1 != null) {
                                winnerId = (String) p1.get("uid");
                                winnerColor = "Red";
                            }
                        }

                        if (winnerId != null) {
                            System.out.println("!!! FORFEIT !!! User " + disconnectedUserId + " did not reconnect. " + winnerColor + " wins!");
                            endGame(gameId, winnerId, loserId, winnerColor, "DISCONNECT");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 30, TimeUnit.SECONDS);
        }
    }
    /**
     * Hàm trợ giúp để xác định người thắng/thua từ trạng thái trận đấu.
     * @param matchState DocumentSnapshot của trận đấu.
     * @param winnerColor Màu của người chiến thắng ("Red" hoặc "Black").
     * @return Một Map chứa "winnerId" và "loserId".
     */
    private Map<String, String> getWinnerAndLoserIds(DocumentSnapshot matchState, String winnerColor) {
        if (matchState == null) return null;

        String loserColor = "Red".equals(winnerColor) ? "Black" : "Red";

        Map<String, Object> winnerPlayerData = (Map<String, Object>) matchState.get(winnerColor.equals("Red") ? "player1" : "player2");
        Map<String, Object> loserPlayerData = (Map<String, Object>) matchState.get(loserColor.equals("Red") ? "player1" : "player2");

        if (winnerPlayerData == null || loserPlayerData == null) return null;

        Map<String, String> result = new HashMap<>();
        result.put("winnerId", (String) winnerPlayerData.get("uid"));
        result.put("loserId", (String) loserPlayerData.get("uid"));
        
        return result;
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
        System.out.println("[SERVER LOG] BROADCASTING to game " + gameId);
        Map<String, Session> sessionsInGame = gameSessions.get(gameId);
        if (sessionsInGame != null && !sessionsInGame.isEmpty()) {
            System.out.println("[SERVER LOG] Found " + sessionsInGame.size() + " users to broadcast to.");
            // Bây giờ, chúng ta duyệt qua các Session trực tiếp
            for (Session session : sessionsInGame.values()) {
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("[SERVER LOG] Step 4: Broadcast successful.");
        } else {
            System.err.println("[SERVER LOG] FATAL ERROR: No client sessions found for game " + gameId);
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
    
    private void endGameAsDraw(String gameId, String reason) {
        System.out.println("\n=============================================");
        System.out.println("[SERVER LOG] Step 1: Entering endGameAsDraw function for game " + gameId);
        System.out.println("[SERVER LOG] Reason: " + reason);
        cancelAfkCheck(gameId);
        try {
            // Lấy trạng thái MỚI NHẤT của trận đấu sau khi đã được cập nhật là COMPLETED
            DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
            if (matchState == null) {
                System.err.println("[SERVER LOG] ERROR: Match " + gameId + " not found in endGameAsDraw.");
                return;
            }
            
            // Cập nhật chỉ số (hòa) cho cả hai người chơi
            Map<String, Object> p1 = (Map<String, Object>) matchState.get("player1");
            Map<String, Object> p2 = (Map<String, Object>) matchState.get("player2");
            if (p1 != null && p2 != null) {
                // SỬA LẠI: Truyền matchId vào hàm này
                UserService.updateUserStatsAfterMatch(gameId, (String)p1.get("uid"), (String)p2.get("uid"), true);
                System.out.println("[SERVER LOG] Step 2: Database updates for user stats (draw) completed.");
            }

            // Tạo và gửi tin nhắn kết thúc game
            Map<String, Object> gameOverMessage = new HashMap<>();
            gameOverMessage.put("type", "GAME_OVER");
            gameOverMessage.put("winnerColor", null); // Quan trọng: null cho biết là hòa
            gameOverMessage.put("reason", reason);
            
            String jsonMessage = gson.toJson(gameOverMessage);
            System.out.println("[SERVER LOG] Step 3: Preparing to broadcast GAME_OVER (draw) message: " + jsonMessage);
            broadcast(gameId, jsonMessage);

        } catch (Exception e) {
            System.err.println("[SERVER LOG] CRITICAL ERROR in endGameAsDraw:");
            e.printStackTrace();
        }
    }

    private void handleResignMessage(Session session, String gameId) throws Exception {
        String resigningUserId = (String) session.getUserProperties().get("userId");
        System.out.println("Processing resignation for game: " + gameId + " from user: " + resigningUserId);

        DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
        if (matchState == null || !"IN_PROGRESS".equals(matchState.getString("status"))) {
            return; // Trận đấu đã kết thúc
        }

        Map<String, Object> p1 = (Map<String, Object>) matchState.get("player1");
        Map<String, Object> p2 = (Map<String, Object>) matchState.get("player2");

        String winnerId = null;
        String loserId = null;
        String winnerColor = null;

        if (p1 != null && resigningUserId.equals(p1.get("uid"))) {
            loserId = (String) p1.get("uid");
            if (p2 != null) {
                winnerId = (String) p2.get("uid");
                winnerColor = "Black";
            }
        } else if (p2 != null && resigningUserId.equals(p2.get("uid"))) {
            loserId = (String) p2.get("uid");
            if (p1 != null) {
                winnerId = (String) p1.get("uid");
                winnerColor = "Red";
            }
        }
        
        if (winnerId != null && loserId != null) {
            System.out.println("!!! RESIGNATION !!! " + winnerColor + " wins!");
            endGame(gameId, winnerId, loserId, winnerColor, "RESIGN");
        }
    }
    
    private void scheduleAfkCheck(String gameId) {
        // Hủy tác vụ cũ (nếu có) trước khi tạo tác vụ mới
        cancelAfkCheck(gameId);

        // Lên lịch một tác vụ mới sẽ chạy sau 90 giây
        ScheduledFuture<?> task = afkScheduler.schedule(() -> {
            try {
                System.out.println("[AFK CHECKER] Running AFK check for game: " + gameId);
                DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
                
                // Chỉ xử lý khi trận đấu đang diễn ra
                if (matchState != null && "IN_PROGRESS".equals(matchState.getString("status"))) {
                    String loserColor = matchState.getString("currentTurn");
                    String winnerColor = "Red".equals(loserColor) ? "Black" : "Red";
                    
                    System.out.println("!!! AFK TIMEOUT !!! Player " + loserColor + " did not move. " + winnerColor + " wins!");
                    
                    Map<String, String> players = getWinnerAndLoserIds(matchState, winnerColor);
                    if (players != null) {
                        endGame(gameId, players.get("winnerId"), players.get("loserId"), winnerColor, "AFK_TIMEOUT");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error during AFK check for game " + gameId);
                e.printStackTrace();
            }
        }, 90, TimeUnit.SECONDS);

        // Lưu lại tác vụ này để có thể hủy nó sau
        afkTasks.put(gameId, task);
    }

    private void cancelAfkCheck(String gameId) {
        ScheduledFuture<?> existingTask = afkTasks.get(gameId);
        if (existingTask != null) {
            // Hủy tác vụ mà không làm gián đoạn nếu nó đang chạy
            existingTask.cancel(false); 
            afkTasks.remove(gameId);
            System.out.println("[AFK CHECKER] Canceled previous AFK check for game: " + gameId);
        }
    }
}