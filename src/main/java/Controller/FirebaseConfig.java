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
        System.out.println("FirebaseConfig: Starting initialization...");

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = null;

                // 1. ƯU TIÊN: Kiểm tra biến môi trường GOOGLE_CREDENTIALS_JSON trước
                String envCredentials = System.getenv("GOOGLE_CREDENTIALS_JSON");

                if (envCredentials != null && !envCredentials.isEmpty()) {
                    System.out.println(
                            "FirebaseConfig: Found GOOGLE_CREDENTIALS_JSON environment variable. Loading from string...");
                    // Chuyển chuỗi JSON thành InputStream
                    InputStream envStream = new ByteArrayInputStream(envCredentials.getBytes(StandardCharsets.UTF_8));
                    credentials = GoogleCredentials.fromStream(envStream);
                }
                // 2. DỰ PHÒNG: Nếu không có biến môi trường, tìm file trong WEB-INF
                else {
                    System.out.println(
                            "FirebaseConfig: Environment variable not found. Looking for serviceAccountKey.json in WEB-INF...");
                    ServletContext context = sce.getServletContext();
                    InputStream serviceAccountStream = context.getResourceAsStream("/WEB-INF/serviceAccountKey.json");

                    if (serviceAccountStream != null) {
                        System.out.println("FirebaseConfig: Found serviceAccountKey.json file.");
                        credentials = GoogleCredentials.fromStream(serviceAccountStream);
                    } else {
                        System.err.println("CRITICAL ERROR: Could not find credentials in ENV or File.");
                    }
                }

                // 3. Kiểm tra xem đã tải được credentials chưa
                if (credentials == null) {
                    throw new IOException(
                            "Firebase configuration failed: No credentials found (Checked GOOGLE_CREDENTIALS_JSON and /WEB-INF/serviceAccountKey.json).");
                }

                // 4. Khởi tạo Firebase
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
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
    }
}
