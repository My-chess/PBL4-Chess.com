package Controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.io.InputStream;

@WebListener
public class FirebaseConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("FirebaseConfig: Initializing Firebase Admin SDK from serviceAccountKey.json for Tomcat...");
        
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Lấy ServletContext để có thể truy cập tài nguyên của ứng dụng
                ServletContext context = sce.getServletContext();

                // Tạo một InputStream để đọc trực tiếp file serviceAccountKey.json
                InputStream serviceAccountStream = context.getResourceAsStream("/WEB-INF/serviceAccountKey.json");

                // Kiểm tra xem file có được tìm thấy không
                if (serviceAccountStream == null) {
                    System.err.println("CRITICAL ERROR: Could not find serviceAccountKey.json inside /WEB-INF/ directory.");
                    throw new IOException("Cannot find serviceAccountKey.json. Make sure it's in the WEB-INF folder.");
                }
                System.out.println("Successfully found serviceAccountKey.json.");

                // Khởi tạo Firebase từ InputStream
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK has been initialized successfully for Tomcat environment.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Firebase Admin SDK initialization failed.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) { }
}