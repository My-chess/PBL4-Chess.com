package Controller;

import Model.DAO.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
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
        String password = request.getParameter("password");

        try {
            // Bước 1: Tạo yêu cầu tạo người dùng mới cho Firebase Authentication
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(username)
                    .setEmailVerified(false); // Bạn có thể thêm bước xác thực email sau

            // Bước 2: Gọi API của Firebase Admin SDK để tạo người dùng
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
            System.out.println("Successfully created new user: " + userRecord.getUid());

            // Bước 3: Sau khi tạo người dùng thành công trên Authentication,
            // tạo một document tương ứng trong collection "users" của Firestore.
            // Hàm createUser của bạn cần được sửa lại một chút.
            UserService.createUser(userRecord.getUid(), email, username);

            // Bước 4: Chuyển hướng đến trang đăng nhập với thông báo thành công
            response.sendRedirect(request.getContextPath() + "/login.jsp?success=true");

        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            String errorMessage;
            // Bắt các lỗi phổ biến từ Firebase Auth để đưa ra thông báo thân thiện
            switch (e.getErrorCode()) {
                case ALREADY_EXISTS:
                    errorMessage = "Email này đã được sử dụng. Vui lòng chọn email khác.";
                    break;
                case INVALID_ARGUMENT:
                    errorMessage = "Mật khẩu không hợp lệ. Mật khẩu phải có ít nhất 6 ký tự.";
                    break;
                default:
                    errorMessage = "Đăng ký thất bại. Lỗi không xác định.";
                    break;
            }
            request.setAttribute("errorMessage", errorMessage);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        } catch (Exception e) {
             e.printStackTrace();
            request.setAttribute("errorMessage", "Đã có lỗi phía server xảy ra.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }
}