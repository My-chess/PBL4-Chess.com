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

        UserBEAN player2 = (UserBEAN) session.getAttribute("loggedInUser");
        String player2Id = player2.getUid();
        String player2DisplayName = player2.getUsername();

        try {
            DocumentSnapshot match = FirebaseService.getMatch(gameId);
            if (match != null && match.get("player2") == null) {
                // Kiểm tra xem người chơi có đang cố tham gia lại ván cờ của chính mình không
                Map<String, Object> player1Data = (Map<String, Object>) match.get("player1");
                if (player1Data != null && player1Data.get("uid").equals(player2Id)) {
                    // Cho phép người chơi 1 mở lại ván cờ họ đã tạo
                    response.sendRedirect(request.getContextPath() + "/game.jsp?gameId=" + gameId);
                    return;
                }

                // Phòng hợp lệ và còn trống
                FirebaseService.joinMatch(gameId, player2Id, player2DisplayName);
                response.sendRedirect(request.getContextPath() + "/game.jsp?gameId=" + gameId);
            } else {
                // Phòng không tồn tại hoặc đã đủ người
                String error = (match == null) ? "Mã phòng không hợp lệ." : "Phòng đã đủ 2 người chơi.";
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
