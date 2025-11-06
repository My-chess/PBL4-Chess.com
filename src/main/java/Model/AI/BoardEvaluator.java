package Model.AI;

import Model.BEAN.*;

public class BoardEvaluator {

    private static final int KING_VALUE = 10000;
    private static final int ROOK_VALUE = 90;
    private static final int KNIGHT_VALUE = 40;
    private static final int CANNON_VALUE = 45;
    private static final int BISHOP_VALUE = 20;
    private static final int ADVISOR_VALUE = 20;
    private static final int PAWN_VALUE = 10;
    
    public int evaluate(Board board, String aiColor) {
        int score = 0;
        PieceBEAN[][] grid = board.getGrid();

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN piece = grid[y][x];
                if (piece != null) {
                    int pieceValue = getPieceValue(piece);
 
                    pieceValue += getPositionalValue(piece, x, y);

                    if (piece.getColor().equals(aiColor)) {
                        score += pieceValue;
                    } else {
                        score -= pieceValue;
                    }
                }
            }
        }
        return score;
    }

    private int getPieceValue(PieceBEAN piece) {
        if (piece instanceof KingBEAN) return KING_VALUE;
        if (piece instanceof RookBEAN) return ROOK_VALUE;
        if (piece instanceof KnightBEAN) return KNIGHT_VALUE;
        if (piece instanceof CannonBEAN) return CANNON_VALUE;
        if (piece instanceof BishopBEAN) return BISHOP_VALUE;
        if (piece instanceof AdvisorBEAN) return ADVISOR_VALUE;
        if (piece instanceof PawnBEAN) return PAWN_VALUE;
        return 0;
    }
 
    private int getPositionalValue(PieceBEAN piece, int x, int y) {
        if (piece instanceof PawnBEAN) {
            if (piece.getColor().equals("Red")) {
                if (y < 5) return 5; 
            } else {
                if (y > 4) return 5;
            }
        } 
        return 0;
    }
}