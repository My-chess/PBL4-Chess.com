package Controller;

import Model.BEAN.MoveBEAN;
import Model.DAO.FirebaseService;
import com.google.gson.Gson; // Bạn cần thêm thư viện Gson nếu chưa có
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

// Ánh xạ servlet này tới một URL bắt đầu bằng /api/
@WebServlet("/api/moves")
public class MoveHistoryApiServlet extends HttpServlet {

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
            List<MoveBEAN> moves = FirebaseService.getMovesForMatch(matchId);
            
            // Chuyển đổi danh sách moves thành chuỗi JSON
            String jsonResponse = gson.toJson(moves);

            // Thiết lập header cho response và gửi đi
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể lấy lịch sử nước đi.");
        }
    }
}