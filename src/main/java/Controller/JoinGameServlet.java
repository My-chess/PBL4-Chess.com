package Controller;

import Model.BEAN.UserBEAN;
import Model.DAO.FirebaseService;
import com.google.cloud.firestore.DocumentSnapshot;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebServlet("/joinGame")
public class JoinGameServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String gameId = request.getParameter("gameId");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        UserBEAN joiningPlayer = (UserBEAN) session.getAttribute("loggedInUser");
        String joiningPlayerId = joiningPlayer.getUid();
        String joiningPlayerDisplayName = joiningPlayer.getUsername();

        try {
            DocumentSnapshot match = FirebaseService.getMatch(gameId);

            // --- THAY ĐỔI QUAN TRỌNG Ở ĐÂY ---
            // Cách kiểm tra đúng: Phòng phải tồn tại VÀ đang ở trạng thái chờ.
            if (match != null && "WAITING".equals(match.getString("status"))) {
                
                // Vẫn giữ lại logic kiểm tra người chơi tự tham gia lại phòng của mình
                Map<String, Object> player1Data = (Map<String, Object>) match.get("player1");
                Map<String, Object> player2Data = (Map<String, Object>) match.get("player2");

                if ((player1Data != null && player1Data.get("uid").equals(joiningPlayerId)) ||
                    (player2Data != null && player2Data.get("uid").equals(joiningPlayerId))) {
                    // Cho phép người chơi mở lại ván cờ họ đã tạo
                    response.sendRedirect(request.getContextPath() + "/game.jsp?gameId=" + gameId);
                    return;
                }

                // Phòng hợp lệ và còn trống, gọi hàm joinMatch
                FirebaseService.joinMatch(gameId, joiningPlayerId, joiningPlayerDisplayName);
                response.sendRedirect(request.getContextPath() + "/game.jsp?gameId=" + gameId);

            } else {
                // Phòng không tồn tại hoặc không còn ở trạng thái chờ (đã bắt đầu hoặc đã kết thúc)
                String error = (match == null) ? "Mã phòng không hợp lệ." : "Phòng đã đủ 2 người chơi hoặc trận đấu đã bắt đầu.";
                request.setAttribute("errorMessage", error);
                request.getRequestDispatcher("/index.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Lỗi server khi tham gia ván cờ.");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }
}