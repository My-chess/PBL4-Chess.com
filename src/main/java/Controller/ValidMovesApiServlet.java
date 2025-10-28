package Controller;

import Model.DAO.FirebaseService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/validMoves")
public class ValidMovesApiServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String matchId = request.getParameter("matchId");
        int pieceX = Integer.parseInt(request.getParameter("x"));
        int pieceY = Integer.parseInt(request.getParameter("y"));

        try {
            List<int[]> moves = FirebaseService.getValidMovesForPiece(matchId, pieceX, pieceY);
            String jsonResponse = gson.toJson(moves);
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}