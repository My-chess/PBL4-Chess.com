package Controller;

import Model.BEAN.UserBEAN;
import Model.DAO.FirebaseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/startGame")
public class StartGameServlet extends HttpServlet {
    
    // THAY ĐỔI: Chuyển từ doGet sang doPost
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        try {
            // Lấy thông tin người chơi từ session
            UserBEAN player1 = (UserBEAN) session.getAttribute("loggedInUser");
            String player1Id = player1.getUid();
            String player1DisplayName = player1.getUsername();

            // Lấy các cài đặt từ form
            int timeMinutes = Integer.parseInt(request.getParameter("timeControl"));
            String sidePreference = request.getParameter("sidePreference");
            boolean isRanked = "true".equals(request.getParameter("isRanked")); // Checkbox chỉ gửi value nếu được check

            // Chuyển đổi phút sang mili giây
            long initialTimeMs = timeMinutes * 60 * 1000L;
            
            // Gọi hàm service đã được cập nhật
            String newGameId = FirebaseService.createNewMatch(player1Id, player1DisplayName, initialTimeMs, isRanked, sidePreference);
            
            response.sendRedirect(request.getContextPath() + "/game.jsp?gameId=" + newGameId);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Lỗi server khi tạo ván cờ.");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }
}