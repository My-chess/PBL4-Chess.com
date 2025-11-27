package Controller;

import Model.AI.BoardEvaluator;
import Model.AI.MinimaxService;
import Model.BEAN.Board;
import Model.BEAN.MoveBEAN;
import Model.BEAN.PieceBEAN;
import Model.DAO.FirebaseService;  
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/ai/newGame", "/api/ai/move", "/api/ai/resign"})
public class AIController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson gson = new Gson();
    private MinimaxService minimaxService = new MinimaxService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            switch (path) {
                case "/ai/newGame":
                    handleNewGame(request, response);
                    break;
                case "/api/ai/move":
                    handlePlayerMove(request, response);
                    break;
                case "/api/ai/resign":
                    handleResign(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonError(response, "Đã có lỗi nghiêm trọng xảy ra ở server: " + e.getMessage());
        }
    }

 
    private void handleNewGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String playerColor = request.getParameter("playerColor");
        String difficulty = request.getParameter("difficulty");

        HttpSession session = request.getSession(true);
        Board board = new Board();  
        String aiColor = playerColor.equals("Red") ? "Black" : "Red";
 
        session.setAttribute("ai_board", board);
        session.setAttribute("ai_player_color", playerColor);
        session.setAttribute("ai_color", aiColor);
        session.setAttribute("ai_difficulty", difficulty);
        session.setAttribute("ai_game_over", false);
        
        // Tạo danh sách lịch sử bàn cờ để kiểm tra lặp lại (Hòa)
        List<String> boardHistory = new ArrayList<>();
        boardHistory.add(generateBoardHash(board));
        session.setAttribute("ai_board_history", boardHistory);
        
        Map<String, String> initialBoardStateMap = FirebaseService.boardToMap(board);
        String jsonBoardState = gson.toJson(initialBoardStateMap);
 
        response.setContentType("text/html");
        response.getWriter().write("<script>"
                + "sessionStorage.setItem('ai_initial_board', '" + jsonBoardState.replace("'", "\\'") + "');"
                + "window.location='" + request.getContextPath() + "/ai_game.jsp';"
                + "</script>");
    }
 
    @SuppressWarnings("unchecked")
    private void handlePlayerMove(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("ai_board") == null) {
            sendJsonError(response, "Session expired.");
            return;
        }

        Board board = (Board) session.getAttribute("ai_board");
        String aiColor = (String) session.getAttribute("ai_color");
        String difficulty = (String) session.getAttribute("ai_difficulty");
        List<String> boardHistory = (List<String>) session.getAttribute("ai_board_history");

        // 1. Đọc dữ liệu từ người chơi
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Type type = new TypeToken<Map<String, Integer>>(){}.getType();
        Map<String, Integer> moveData = gson.fromJson(requestBody, type);

        int startX = moveData.get("startX");
        int startY = moveData.get("startY");
        int endX = moveData.get("endX");
        int endY = moveData.get("endY");

        // 2. Xử lý nước đi của Người chơi (nếu không phải lượt đầu máy đi trước)
        if (startX != -1) { 
        	if (!board.isWithinBounds(startX, startY) || !board.isWithinBounds(endX, endY)) {
                sendJsonError(response, "Tọa độ không hợp lệ (ngoài bàn cờ).");
                return;
            }
            if (!board.isMoveValid(startX, startY, endX, endY)) {
                sendJsonError(response, "Nước đi không hợp lệ.");
                return;
            }
            board.executeMove(startX, startY, endX, endY);
            
            // Kiểm tra Người chơi thắng (Máy bị chiếu bí)
            if (!board.hasLegalMoves(aiColor)) {
                String reason = board.isKingInCheck(aiColor) ? "CHECKMATE" : "STALEMATE";
                endGame(session, response, board, reason, null); // Người chơi thắng
                return;
            }
        }

        // 3. Máy tính toán nước đi (AI Turn)
        // Kiểm tra xem máy có nước đi nào không (Stalemate)
        MoveBEAN aiMove = minimaxService.findBestMove(board, difficulty, aiColor);
        
        if (aiMove == null) {
            // Máy không còn nước đi hợp lệ -> Máy thua (Trong cờ tướng hết nước là thua)
        	endGame(session, response, board, "STALEMATE", null); 
            return;
        }

        // Thực hiện nước đi của AI
        board.executeMove(aiMove.getStartX(), aiMove.getStartY(), aiMove.getEndX(), aiMove.getEndY());

        // 4. Kiểm tra các điều kiện kết thúc game sau khi AI đi
        String playerColor = aiColor.equals("Red") ? "Black" : "Red";
        
        // A. Kiểm tra Máy thắng (Người chơi bị chiếu bí)
        if (!board.hasLegalMoves(playerColor)) {
            String reason = board.isKingInCheck(playerColor) ? "CHECKMATE" : "STALEMATE";
            endGame(session, response, board, reason, aiMove); // Máy thắng
            return;
        }

        // B. Kiểm tra Hòa do lặp lại (3 lần lặp lại trạng thái bàn cờ)
        String currentHash = generateBoardHash(board);
        boardHistory.add(currentHash);
        if (isRepeated(boardHistory, currentHash)) {
            endGame(session, response, board, "DRAW", aiMove);
            return;
        }

        // Cập nhật session
        session.setAttribute("ai_board", board);
        session.setAttribute("ai_board_history", boardHistory);

        // 5. Gửi phản hồi về client
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("success", true);
        responseData.put("aiMove", aiMove);
        responseData.put("newBoardState", FirebaseService.boardToMap(board));
        responseData.put("gameState", "IN_PROGRESS");
        
        sendJsonResponse(response, responseData);
    }
    
    // Hàm băm đơn giản trạng thái bàn cờ để kiểm tra lặp lại
    private String generateBoardHash(Board board) {
        StringBuilder sb = new StringBuilder();
        PieceBEAN[][] grid = board.getGrid();
        for(int y=0; y<10; y++) {
            for(int x=0; x<9; x++) {
                PieceBEAN p = grid[y][x];
                if(p != null) sb.append(p.getColor().charAt(0)).append(p.getClass().getSimpleName().charAt(0)).append(x).append(y);
            }
        }
        return sb.toString();
    }
    
    // Kiểm tra lặp lại 3 lần
    private boolean isRepeated(List<String> history, String currentHash) {
        int count = 0;
        for (String state : history) {
            if (state.equals(currentHash)) count++;
        }
        return count >= 3; // Lặp lại 3 lần là hòa
    }
    private void endGame(HttpSession session, HttpServletResponse response, Board board, String reason, MoveBEAN aiMove) throws IOException {
        session.setAttribute("ai_game_over", true);
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("success", true);
        if (aiMove != null) responseData.put("aiMove", aiMove);
        responseData.put("newBoardState", FirebaseService.boardToMap(board));
        responseData.put("gameState", reason); // "CHECKMATE" hoặc "DRAW"
        sendJsonResponse(response, responseData);
    }
 
    private void handleResign(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ai_board") != null) {
            session.setAttribute("ai_game_over", true);
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Người chơi đã đầu hàng.");
            sendJsonResponse(response, responseData);
        } else {
            sendJsonError(response, "Không có ván cờ nào để đầu hàng.");
        }
    }
 

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(gson.toJson(data));
    }

    private void sendJsonError(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        sendJsonResponse(response, errorResponse);
    }

    private void sendGameEndResponse(HttpServletResponse response, Board board, String gameState, MoveBEAN aiMove) throws IOException {
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("success", true);
        responseData.put("aiMove", aiMove);  
        responseData.put("newBoardState", FirebaseService.boardToMap(board));
        responseData.put("gameState", gameState);
        sendJsonResponse(response, responseData);
    }
}