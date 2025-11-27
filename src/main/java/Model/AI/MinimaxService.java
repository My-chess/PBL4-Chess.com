package Model.AI;

import Model.BEAN.Board;
import Model.BEAN.MoveBEAN;
import Model.BEAN.PieceBEAN;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinimaxService {

    private BoardEvaluator boardEvaluator = new BoardEvaluator();

    public MoveBEAN findBestMove(Board board, String difficulty, String aiColor) {
        int depth = getDepthFromDifficulty(difficulty);
        List<MoveBEAN> allPossibleMoves = getAllPossibleMoves(board, aiColor);
        
        if (allPossibleMoves.isEmpty()) {
            return null;  
        }

        MoveBEAN bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (MoveBEAN move : allPossibleMoves) {
            Board newBoard = new Board(board);  
            newBoard.executeMove(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());

            int boardValue = minimax(newBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiColor);
            
            if (boardValue > bestValue) {
                bestValue = boardValue;
                bestMove = move;
            }
        }
        return bestMove;
    }
 // Hàm gốc để gọi đệ quy
    private MoveBEAN minimaxRoot(Board board, int depth, boolean isMaximizingPlayer, String aiColor) {
        List<MoveBEAN> allMoves = getAllPossibleMoves(board, aiColor);
        
        // Sắp xếp nước đi để cắt tỉa Alpha-Beta tốt hơn (ví dụ: ưu tiên ăn quân trước)
        // Ở đây để đơn giản ta xáo trộn để AI không bị lặp lại nước đi quá máy móc
        Collections.shuffle(allMoves); 

        MoveBEAN bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (MoveBEAN move : allMoves) {
            Board newBoard = new Board(board);
            newBoard.executeMove(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());

            int boardValue = minimax(newBoard, depth - 1, alpha, beta, false, aiColor);

            if (boardValue > bestValue) {
                bestValue = boardValue;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestValue);
        }
        return bestMove;
    }

    private int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizingPlayer, String aiColor) {
        String opponentColor = aiColor.equals("Red") ? "Black" : "Red";

        // KIỂM TRA ĐIỀU KIỆN KẾT THÚC SỚM
        if (board.isCheckmate(aiColor)) return -99999; // AI thua
        if (board.isCheckmate(opponentColor)) return 99999; // AI thắng

        if (depth == 0) {
            return boardEvaluator.evaluate(board, aiColor);
        }

        String currentColor = isMaximizingPlayer ? aiColor : opponentColor;
        List<MoveBEAN> allMoves = getAllPossibleMoves(board, currentColor);

        if (isMaximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (MoveBEAN move : allMoves) {
                Board newBoard = new Board(board);
                newBoard.executeMove(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());
                int eval = minimax(newBoard, depth - 1, alpha, beta, false, aiColor);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (MoveBEAN move : allMoves) {
                Board newBoard = new Board(board);
                newBoard.executeMove(move.getStartX(), move.getStartY(), move.getEndX(), move.getEndY());
                int eval = minimax(newBoard, depth - 1, alpha, beta, true, aiColor);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private List<MoveBEAN> getAllPossibleMoves(Board board, String color) {
        List<MoveBEAN> moves = new ArrayList<>();
        PieceBEAN[][] grid = board.getGrid();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN piece = grid[y][x];
                if (piece != null && piece.getColor().equals(color)) {
                    for (int endY = 0; endY < 10; endY++) {
                        for (int endX = 0; endX < 9; endX++) {
                            if (board.isMoveValid(x, y, endX, endY)) {
                                moves.add(new MoveBEAN(color, x, y, endX, endY, piece.getClass().getSimpleName(), null));
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

    private int getDepthFromDifficulty(String difficulty) {
        switch (difficulty) {
            case "Easy":
                return 2;
            case "Hard":
                return 4;
            case "Medium":
            default:
                return 3;
        }
    }
}