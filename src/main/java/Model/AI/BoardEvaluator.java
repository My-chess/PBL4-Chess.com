package Model.AI;

import Model.BEAN.*;

public class BoardEvaluator {

    // Giá trị cơ bản của quân cờ
    private static final int KING_VALUE = 10000;
    private static final int ROOK_VALUE = 1000;
    private static final int KNIGHT_VALUE = 450;
    private static final int CANNON_VALUE = 500;
    private static final int BISHOP_VALUE = 250;
    private static final int ADVISOR_VALUE = 200;
    private static final int PAWN_VALUE = 100; // Tăng giá trị tốt lên để AI biết giữ tốt

    // Mảng điểm vị trí cho Tốt (Pawn) - Khuyến khích tốt qua sông và tiến xuống
    private static final int[][] PAWN_TABLE = {
        {  0,  0,  0,  0,  0,  0,  0,  0,  0 },
        { 90, 90,110,120,120,120,110, 90, 90 }, // Áp sát cung tướng
        { 90, 90,110,120,120,120,110, 90, 90 },
        { 70, 90,100,110,110,110,100, 90, 70 },
        { 50, 50, 60, 80, 80, 80, 60, 50, 50 }, // Qua sông
        { 10, 10, 20, 30, 30, 30, 20, 10, 10 }, // Bên nhà
        { 10, 10, 20, 30, 30, 30, 20, 10, 10 },
        {  0,  0,  0,  0,  0,  0,  0,  0,  0 },
        {  0,  0,  0,  0,  0,  0,  0,  0,  0 },
        {  0,  0,  0,  0,  0,  0,  0,  0,  0 }
    };

    // Mảng điểm vị trí cho Xe (Rook) - Khuyến khích chiếm lộ thông thoáng
    private static final int[][] ROOK_TABLE = {
        { 14, 14, 12, 18, 16, 18, 12, 14, 14 },
        { 16, 20, 18, 24, 26, 24, 18, 20, 16 },
        { 12, 12, 12, 18, 18, 18, 12, 12, 12 },
        { 12, 18, 16, 22, 22, 22, 16, 18, 12 },
        { 12, 14, 12, 18, 18, 18, 12, 14, 12 },
        { 12, 16, 14, 20, 20, 20, 14, 16, 12 },
        {  6, 10,  8, 14, 14, 14,  8, 10,  6 },
        {  4,  8,  6, 14, 12, 14,  6,  8,  4 },
        {  8,  4,  8, 16,  8, 16,  8,  4,  8 },
        { -2, 10,  6, 14, 12, 14,  6, 10, -2 }
    };

    // Mảng điểm vị trí cho Mã (Knight) - Khuyến khích nhảy về phía trung tâm và tiến lên
    private static final int[][] KNIGHT_TABLE = {
        { 12, 16, 14, 18, 18, 18, 14, 16, 12 },
        { 12, 16, 16, 20, 24, 20, 16, 16, 12 },
        { 10, 14, 18, 22, 26, 22, 18, 14, 10 },
        { 10, 16, 20, 24, 24, 24, 20, 16, 10 },
        {  8, 12, 16, 20, 22, 20, 16, 12,  8 },
        {  8, 10, 14, 18, 20, 18, 14, 10,  8 },
        {  6,  8, 12, 16, 18, 16, 12,  8,  6 },
        {  4,  6,  8, 12, 14, 12,  8,  6,  4 },
        {  2,  4,  6,  8,  8,  8,  6,  4,  2 },
        { -4,  2,  4,  6,  6,  6,  4,  2, -4 }
    };

    // Mảng điểm vị trí cho Pháo (Cannon) - Ưu tiên hàng 2/hàng 7 (pháo gánh)
    private static final int[][] CANNON_TABLE = {
        { 6,  6,  6, 10, 12, 10,  6,  6,  6 },
        { 6,  4,  0, -4, -6, -4,  0,  4,  6 }, // Tránh bị cản
        { 4,  2,  4,  4, 10,  4,  4,  2,  4 },
        { 6,  6,  6, 12, 14, 12,  6,  6,  6 },
        { 0,  2,  6,  6,  8,  6,  6,  2,  0 },
        { 0,  0,  0,  2,  4,  2,  0,  0,  0 },
        { 0,  2,  4,  6,  6,  6,  4,  2,  0 },
        { 4,  2,  8,  8, 12,  8,  8,  2,  4 },
        { 0,  2,  4,  4,  4,  4,  4,  2,  0 },
        { 0,  0,  2,  4,  4,  4,  2,  0,  0 }
    };

    public int evaluate(Board board, String aiColor) {
        int score = 0;
        PieceBEAN[][] grid = board.getGrid();
        String opponentColor = aiColor.equals("Red") ? "Black" : "Red";

        // 1. Nếu AI đang chiếu tướng đối phương -> Cộng điểm cực lớn
        if (board.isKingInCheck(opponentColor)) {
            score += 150; // Thưởng cho việc chiếu tướng (giúp AI biết cách dồn ép)
        }

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                PieceBEAN piece = grid[y][x];
                if (piece != null) {
                    int pieceValue = getPieceValue(piece);
                    int positionalValue = getPositionalValue(piece, x, y, piece.getColor());

                    if (piece.getColor().equals(aiColor)) {
                        score += (pieceValue + positionalValue);
                    } else {
                        score -= (pieceValue + positionalValue);
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

    private int getPositionalValue(PieceBEAN piece, int x, int y, String color) {
        // Cần đảo ngược tọa độ Y nếu là quân Đỏ (giả sử bảng trên tính theo góc nhìn quân Đen/Quân ở trên đi xuống)
        // Trong Board của bạn: y=0 là Đen, y=9 là Đỏ.
    	if (x < 0 || x > 8 || y < 0 || y > 9) return 0;
        int row = y;
        if (color.equals("Red")) {
            row = 9 - y; // Đảo ngược hàng để dùng chung bảng điểm
        }
        
        if (row < 0 || row > 9) return 0; 
        
        if (piece instanceof PawnBEAN) return PAWN_TABLE[row][x];
        if (piece instanceof RookBEAN) return ROOK_TABLE[row][x];
        if (piece instanceof KnightBEAN) return KNIGHT_TABLE[row][x];
        if (piece instanceof CannonBEAN) return CANNON_TABLE[row][x];
        
        // Tướng, Sĩ, Tượng ít di chuyển nên có thể bỏ qua hoặc thêm bảng đơn giản
        return 0;
    }
}