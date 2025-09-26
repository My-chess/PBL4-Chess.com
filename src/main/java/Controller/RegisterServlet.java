package Controller;

import Model.DAO.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password"); // Bạn cần xử lý password an toàn hơn

        try {
            // TODO: Bổ sung logic kiểm tra xem email đã tồn tại chưa
            // TODO: Tích hợp Firebase Authentication để tạo người dùng thực sự

            // Tạm thời, chúng ta sẽ tạo user trong Firestore
            // Giả sử Firebase Auth trả về một uid
            UserService.createUser(email, username);

            // Nếu thành công, chuyển hướng đến trang đăng nhập với thông báo
            response.sendRedirect(request.getContextPath() + "/login.jsp?success=true");

        } catch (Exception e) {
            e.printStackTrace();
            // Nếu có lỗi, gửi lại trang đăng ký với thông báo lỗi
            request.setAttribute("errorMessage", "Đăng ký thất bại, email có thể đã được sử dụng.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }
}
