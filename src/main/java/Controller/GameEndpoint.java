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
     
     // Bước 1: Trích xuất tọa độ nước đi từ dữ liệu tin nhắn.
     // Dữ liệu được gửi từ client có dạng { type: "move", data: { startX: ..., startY: ... } }
     @SuppressWarnings("unchecked") // Bỏ qua cảnh báo vì chúng ta biết chắc cấu trúc JSON từ client
     Map<String, Double> movePayload = (Map<String, Double>) messageData.get("data");
     int startX = movePayload.get("startX").intValue();
     int startY = movePayload.get("startY").intValue();
     int endX = movePayload.get("endX").intValue();
     int endY = movePayload.get("endY").intValue();

     System.out.println("Đang xử lý nước đi cho GameID " + gameId + ": từ [" + startX + "," + startY + "] đến [" + endX + "," + endY + "]");

     // Bước 2: Gọi hàm xử lý tập trung ở tầng Service (FirebaseService).
     // Hàm này sẽ thực hiện toàn bộ logic nặng:
     // - Lấy trạng thái bàn cờ hiện tại từ Firestore.
     // - Tái tạo đối tượng Board.
     // - Xác thực tính hợp lệ của nước đi bằng hàm board.isMoveValid().
     // - Nếu hợp lệ, thực hiện nước đi, tạo trạng thái bàn cờ mới và cập nhật lại lên Firestore.
     // - Hàm sẽ trả về `true` nếu toàn bộ quá trình thành công, và `false` nếu nước đi không hợp lệ.
     boolean isMoveSuccessful = FirebaseService.processMoveAndUpdateBoard(gameId, startX, startY, endX, endY);

     // Bước 3: Xử lý kết quả trả về từ Service.
     if (isMoveSuccessful) {
         
         // --- Trường hợp nước đi hợp lệ và đã được cập nhật ---
         System.out.println("Thành công: Nước đi hợp lệ, bàn cờ đã được cập nhật trên Firestore.");
         
         // Ghi chú quan trọng: Chúng ta không cần gửi lại trạng thái bàn cờ ở đây.
         // Client (game.js) đang lắng nghe (onSnapshot) thay đổi trên document của trận đấu.
         // Khi hàm `processMoveAndUpdateBoard` cập nhật Firestore, client sẽ tự động nhận được `boardState` mới và vẽ lại giao diện.
         // Đây chính là sức mạnh của kiến trúc này.
         
         // Bước 3.1: >>> KIỂM TRA CHIẾU BÍ SAU KHI ĐI THÀNH CÔNG <<<
         // Sau khi nước đi đã được xác nhận và lưu lại, chúng ta cần kiểm tra xem đối thủ có bị chiếu bí không.
         
         // Tải lại trạng thái trận đấu ngay sau khi đã cập nhật để có dữ liệu mới nhất.
         DocumentSnapshot matchStateAfterMove = FirebaseService.getMatch(gameId);
         if (matchStateAfterMove == null) return; // Trận đấu không còn tồn tại, thoát.

         // Xác định người thắng (người vừa đi) và người thua (người có lượt tiếp theo, vì lượt đã được đổi)
         String winnerColor = "Red".equals(matchStateAfterMove.getString("currentTurn")) ? "Black" : "Red";
         String loserColor = matchStateAfterMove.getString("currentTurn");

         // Tái tạo bàn cờ từ trạng thái mới nhất để kiểm tra chiếu bí.
         @SuppressWarnings("unchecked")
         Map<String, String> boardStateMap = (Map<String, String>) matchStateAfterMove.get("boardState");
         Board boardAfterMove = new Board(boardStateMap);

         if (boardAfterMove.isCheckmate(loserColor)) {
             System.out.println("!!! CHIẾU BÍ !!! Người chơi phe " + winnerColor + " đã thắng!");
             
             // Lấy ID của người thắng và người thua từ document trận đấu
             @SuppressWarnings("unchecked")
             String winnerId = ((Map<String, String>)matchStateAfterMove.get(winnerColor.equals("Red") ? "player1" : "player2")).get("uid");
             @SuppressWarnings("unchecked")
             String loserId = ((Map<String, String>)matchStateAfterMove.get(loserColor.equals("Red") ? "player1" : "player2")).get("uid");
             
             // Gọi hàm kết thúc game (cập nhật DB, ELO, và thông báo cho client)
             endGame(gameId, winnerId, loserId, winnerColor, "CHECKMATE");
         }
         
     } else {
         
         // --- Trường hợp nước đi không hợp lệ ---
         System.out.println("Thất bại: Nước đi không hợp lệ. Gửi thông báo lỗi về cho client.");

         // Gửi một tin nhắn lỗi cụ thể về cho client đã gửi nước đi.
         // Client (game.js) đã được lập trình để nhận tin nhắn `ERROR` và hiển thị thông báo cho người dùng.
         // Vì nước đi bị từ chối, trạng thái bàn cờ trên Firestore không hề thay đổi,
         // do đó bàn cờ của người chơi kia cũng không bị ảnh hưởng. Mọi thứ vẫn đồng bộ.
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