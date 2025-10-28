package Controller;

import Model.BEAN.UserBEAN;
import Model.DAO.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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
        // THAY ĐỔI LỚN: Không nhận email/password nữa.
        // Thay vào đó, nhận idToken do Firebase JS SDK ở client gửi lên.
        String idToken = request.getParameter("idToken");

        if (idToken == null || idToken.isEmpty()) {
            request.setAttribute("errorMessage", "Yêu cầu đăng nhập không hợp lệ.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try {
            // Bước 1: Xác thực idToken bằng Firebase Admin SDK
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // Bước 2: Lấy thông tin người dùng từ Firestore bằng uid đã xác thực
            UserBEAN user = UserService.getUser(uid);

            if (user != null) {
                // Đăng nhập thành công, tạo session
                HttpSession session = request.getSession();
                session.setAttribute("loggedInUser", user);
                session.setMaxInactiveInterval(30 * 60); // 30 phút

                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
                // Trường hợp hiếm: user tồn tại trên Auth nhưng không có trong Firestore
                request.setAttribute("errorMessage", "Không tìm thấy dữ liệu người dùng.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Đăng nhập thất bại. Token không hợp lệ hoặc đã hết hạn.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}