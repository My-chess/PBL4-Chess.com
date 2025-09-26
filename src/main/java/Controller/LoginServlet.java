package Controller;

import Model.BEAN.UserBEAN;
import Model.DAO.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            // TODO: Tích hợp Firebase Authentication để xác thực email/password.
            // Logic dưới đây là giả lập: tìm user trong Firestore bằng email
            // Bạn cần bổ sung hàm getUserByEmail trong UserService
            UserBEAN user = UserService.getUserByEmail(email); // Giả sử có hàm này

            if (user != null) {
                // Đăng nhập thành công
                HttpSession session = request.getSession(); // Tạo session mới
                session.setAttribute("loggedInUser", user);
                session.setMaxInactiveInterval(30 * 60); // Session tồn tại trong 30 phút

                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
                // Đăng nhập thất bại
                request.setAttribute("errorMessage", "Email hoặc mật khẩu không chính xác.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Lỗi server khi đăng nhập.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}