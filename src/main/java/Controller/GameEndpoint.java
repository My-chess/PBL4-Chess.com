package Controller;

import Model.BEAN.Board;
import Model.BEAN.MoveBEAN;
import Model.DAO.FirebaseService;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;

@ServerEndpoint("/game/{gameId}")
public class GameEndpoint {
    private static final Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session session, @PathParam("gameId") String gameId) {
        System.out.println("--- WebSocket Connection Opened ---");
        System.out.println("GameID: " + gameId + ", SessionID: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("gameId") String gameId) throws IOException {
        System.out.println("\n--- Message Received on Server ---");
        System.out.println("From GameID: " + gameId);
        System.out.println("Raw Message: " + message);

        try {
            Map<String, Double> moveData = gson.fromJson(message, Map.class);
            int startX = moveData.get("startX").intValue();
            int startY = moveData.get("startY").intValue();
            int endX = moveData.get("endX").intValue();
            int endY = moveData.get("endY").intValue();
            System.out.println("Parsed move: from [" + startX + "," + startY + "] to [" + endX + "," + endY + "]");
            
            DocumentSnapshot matchState = FirebaseService.getMatch(gameId);
            if (matchState == null) {
                System.out.println("Validation FAILED: Match does not exist.");
                sendError(session, "Trận đấu không tồn tại.");
                return;
            }

            // TODO: Bổ sung logic kiểm tra xem có đúng lượt của người chơi này không

            System.out.println("Reconstructing board from moves...");
            Board currentBoard = FirebaseService.reconstructBoardFromMoves(gameId);
            
            System.out.println("Validating move...");
            if (currentBoard.isMoveValid(startX, startY, endX, endY)) {
                System.out.println("Validation SUCCESS: Move is valid.");
                MoveBEAN move = new MoveBEAN(
                    matchState.getString("currentTurn"),
                    startX, startY, endX, endY,
                    currentBoard.getPieceAt(startX, startY).getClass().getSimpleName(),
                    Timestamp.now()
                );
                
                System.out.println("Saving move to Firestore...");
                FirebaseService.saveMoveAndSwitchTurn(gameId, matchState, move);
                System.out.println("Move saved successfully.");
                
                session.getBasicRemote().sendText("{\"type\":\"MOVE_ACCEPTED\"}");
            } else {
                System.out.println("Validation FAILED: Move is NOT valid according to game rules.");
                sendError(session, "Nước đi không hợp lệ!");
            }
        } catch (Exception e) {
            System.out.println("!!! AN EXCEPTION OCCURRED ON SERVER !!!");
            e.printStackTrace(); // In ra lỗi chi tiết trong log của server
            sendError(session, "Lỗi phía server: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("gameId") String gameId) {
        System.out.println("--- WebSocket Connection Closed for GameID: " + gameId + " ---");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("!!! WEBSOCKET ERROR !!!");
        throwable.printStackTrace();
    }

    private void sendError(Session session, String message) throws IOException {
        session.getBasicRemote().sendText(String.format("{\"type\":\"MOVE_FAIL\", \"message\":\"%s\"}", message));
    }
}