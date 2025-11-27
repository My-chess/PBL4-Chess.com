package Controller;

import Model.DAO.FirebaseService;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/matchDetails")
public class MatchDetailsApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String matchId = request.getParameter("matchId");

        if (matchId == null || matchId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu tham số matchId.");
            return;
        }

        try {
            DocumentSnapshot matchDoc = FirebaseService.getMatch(matchId);
            
            if (matchDoc != null && matchDoc.exists()) {
                // Chuyển đổi toàn bộ document thành chuỗi JSON
                String jsonResponse = gson.toJson(matchDoc.getData());

                // Thiết lập header cho response và gửi đi
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(jsonResponse);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy trận đấu.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể lấy thông tin trận đấu.");
        }
    }
}