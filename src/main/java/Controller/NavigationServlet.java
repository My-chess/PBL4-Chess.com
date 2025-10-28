package Controller;

import Model.BEAN.UserBEAN;
import Model.DAO.FirebaseService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

import com.google.cloud.firestore.QueryDocumentSnapshot;

@WebServlet("/app/*")
public class NavigationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String action = request.getPathInfo();
        if (action == null) action = "/";
        
        try {
            switch (action) {
                case "/profile":
                    viewProfile(request, response);
                    break;
                case "/history":
                    viewMatchHistory(request, response);
                    break;
                case "/replay": // <-- THÊM CASE MỚI NÀY
                    viewReplay(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/index.jsp");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Đã có lỗi xảy ra: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    private void viewProfile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/profile.jsp");
        dispatcher.forward(request, response);
    }
    
    private void viewMatchHistory(HttpServletRequest request, HttpServletResponse response) 
            throws Exception { // Thay đổi để có thể ném Exception
        HttpSession session = request.getSession(false);
        UserBEAN loggedInUser = (UserBEAN) session.getAttribute("loggedInUser");
        String userId = loggedInUser.getUid();

        // Gọi hàm service để lấy dữ liệu
        List<Model.BEAN.MatchBEAN> matchHistory = FirebaseService.getMatchHistory(userId); 

        // Gửi dữ liệu qua request để JSP có thể truy cập
        request.setAttribute("matchHistory", matchHistory);
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/history.jsp");
        dispatcher.forward(request, response);
    }
    
    private void viewReplay(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Lấy matchId từ URL và gửi nó cho JSP
        String matchId = request.getParameter("matchId");
        request.setAttribute("matchId", matchId);
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/replay.jsp");
        dispatcher.forward(request, response);
    }
}