package Controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lớp cấu hình Firebase, được thiết kế để chạy khi server khởi động.
 * Phiên bản này được tối ưu cho môi trường production (như Render, Heroku)
 * bằng cách đọc thông tin xác thực từ biến môi trường của hệ thống.
 */
@WebListener
public class FirebaseConfig implements ServletContextListener {

    /**
     * Phương thức này được tự động gọi một lần duy nhất khi ứng dụng web bắt đầu chạy.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("FirebaseConfig: Initializing Firebase Admin SDK from Environment Variable...");
        
        try {
            // Chỉ khởi tạo nếu chưa có ứng dụng Firebase nào chạy
            if (FirebaseApp.getApps().isEmpty()) { 
                
                // 1. Đọc nội dung key từ biến môi trường của hệ thống (Render sẽ cung cấp biến này)
                // System.getenv() là cách chuẩn của Java để đọc environment variables.
                String firebaseCredentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");

                // 2. Kiểm tra xem biến môi trường có tồn tại không. Đây là bước gỡ lỗi quan trọng.
                if (firebaseCredentialsJson == null || firebaseCredentialsJson.isEmpty()) {
                    System.err.println("FATAL ERROR: Environment variable GOOGLE_CREDENTIALS_JSON is not set.");
                    throw new RuntimeException("Environment variable GOOGLE_CREDENTIALS_JSON is not set.");
                }
                System.out.println("Successfully loaded credentials from environment variable.");

                // 3. Chuyển chuỗi JSON (dạng String) thành một InputStream mà Google Credentials có thể đọc được
                InputStream serviceAccountStream = new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8));
                
                // 4. Khởi tạo Firebase SDK
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK has been initialized successfully.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Firebase Admin SDK initialization failed.", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Có thể thêm logic dọn dẹp ở đây nếu cần
    }
}
