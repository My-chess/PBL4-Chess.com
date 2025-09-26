package Controller;

import Model.BEAN.UserBEAN;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

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
    
    private void viewMatchHistory(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: Lấy dữ liệu lịch sử đấu từ FirebaseService và gửi qua request
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/history.jsp");
        dispatcher.forward(request, response);
    }
}