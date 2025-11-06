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

        if (playerColor == null || difficulty == null || (!playerColor.equals("Red") && !playerColor.equals("Black"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin màu quân hoặc độ khó.");
            return;
        }

        HttpSession session = request.getSession(true);
        Board board = new Board();  
        String aiColor = playerColor.equals("Red") ? "Black" : "Red";
 
        session.setAttribute("ai_board", board);
        session.setAttribute("ai_player_color", playerColor);
        session.setAttribute("ai_color", aiColor);
        session.setAttribute("ai_difficulty", difficulty);
        session.setAttribute("ai_game_over", false);  
        
        Map<String, String> initialBoardStateMap = FirebaseService.boardToMap(board);
        String jsonBoardState = gson.toJson(initialBoardStateMap);
 
        response.setContentType("text/html");
        response.getWriter().write("<script>"
                + "sessionStorage.setItem('ai_initial_board', '" + jsonBoardState.replace("'", "\\'") + "');"
                + "window.location='" + request.getContextPath() + "/ai_game.jsp';"
                + "</script>");
    }
 
    private void handlePlayerMove(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("ai_board") == null) {
            sendJsonError(response, "Không tìm thấy ván cờ. Vui lòng bắt đầu ván mới.");
            return;
        }

        if ((boolean) session.getAttribute("ai_game_over")) {
            sendJsonError(response, "Ván cờ đã kết thúc.");
            return;
        }
 
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Type type = new TypeToken<Map<String, Integer>>(){}.getType();
        Map<String, Integer> moveData = gson.fromJson(requestBody, type);

        int startX = moveData.get("startX");
        int startY = moveData.get("startY");
        int endX = moveData.get("endX");
        int endY = moveData.get("endY");

        Board board = (Board) session.getAttribute("ai_board");
        String aiColor = (String) session.getAttribute("ai_color");
        String difficulty = (String) session.getAttribute("ai_difficulty");
         
        if (startX == -1) { 
        } else { 
            PieceBEAN pieceToMove = board.getPieceAt(startX, startY);
            if (pieceToMove == null || !board.isMoveValid(startX, startY, endX, endY)) {
                sendJsonError(response, "Nước đi của bạn không hợp lệ.");
                return;
            }
            board.executeMove(startX, startY, endX, endY);
        }
 
        String opponentColor = aiColor.equals("Red") ? "Black" : "Red";
        if (board.isCheckmate(aiColor)) {
            session.setAttribute("ai_game_over", true);
            sendGameEndResponse(response, board, "CHECKMATE", null);
            return;
        } 
        
        
        MoveBEAN aiMove = minimaxService.findBestMove(board, difficulty, aiColor);
        if (aiMove == null) {  
             session.setAttribute("ai_game_over", true);
             sendGameEndResponse(response, board, "CHECKMATE", null); 
             return;
        }
        board.executeMove(aiMove.getStartX(), aiMove.getStartY(), aiMove.getEndX(), aiMove.getEndY());
 
        session.setAttribute("ai_board", board);
 
        if(board.isCheckmate(opponentColor)){
            session.setAttribute("ai_game_over", true);
        }

 
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("success", true);
        responseData.put("aiMove", aiMove);
        responseData.put("newBoardState", FirebaseService.boardToMap(board));
        responseData.put("gameState", (boolean) session.getAttribute("ai_game_over") ? "CHECKMATE" : "IN_PROGRESS");
        
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