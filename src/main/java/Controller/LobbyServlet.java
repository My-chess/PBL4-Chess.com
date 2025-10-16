package Controller;

import Model.DAO.FirebaseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/lobby") // Servlet này sẽ xử lý các yêu cầu đến URL /lobby
public class LobbyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            // 1. Gọi FirebaseService để lấy danh sách các phòng đang chờ
            List<Map<String, Object>> waitingMatches = FirebaseService.getWaitingMatches();

            // 2. Đặt danh sách này vào request attribute để JSP có thể truy cập
            request.setAttribute("waitingMatches", waitingMatches);

            // 3. Chuyển tiếp yêu cầu đến trang lobby.jsp để hiển thị
            request.getRequestDispatcher("/WEB-INF/views/lobby.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            // Có thể chuyển hướng đến một trang lỗi
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải danh sách phòng.");
        }
    }
}