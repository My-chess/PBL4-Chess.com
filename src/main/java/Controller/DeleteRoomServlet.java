package Controller;

import Model.DAO.FirebaseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/deleteWaitingRoom")
public class DeleteRoomServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // === THAY ĐỔI CỐT LÕI: Bỏ hoàn toàn việc kiểm tra session ===
        // Thay vào đó, lấy thông tin trực tiếp từ payload của sendBeacon

        String userId = request.getParameter("userId");
        String gameId = request.getParameter("gameId");

        // Kiểm tra xem client có gửi đủ thông tin không
        if (gameId == null || gameId.isEmpty() || userId == null || userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            // Gọi hàm service để xóa phòng.
            // Việc xác thực sẽ được thực hiện bên trong hàm này.
            FirebaseService.deleteWaitingMatch(gameId, userId);
            
            System.out.println("Yêu cầu xóa phòng chờ '" + gameId + "' từ user '" + userId + "' đã được xử lý.");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý yêu cầu xóa phòng: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}