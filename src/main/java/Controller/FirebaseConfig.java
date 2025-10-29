package Controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@WebListener
public class FirebaseConfig implements ServletContextListener {

    private static final String CREDENTIALS_FILE_NAME = "serviceAccountKey.json";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("FirebaseConfig: Attempting to initialize Firebase Admin SDK...");

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                
                InputStream serviceAccountStream = null;
                
                // === BẮT ĐẦU PHẦN TỐI ƯU HÓA VỚI BIẾN MÔI TRƯỜNG ===

                // 1. Ưu tiên đọc đường dẫn từ biến môi trường "GOOGLE_CREDENTIALS_PATH"
                String credentialsPathFromEnv = System.getenv("GOOGLE_CREDENTIALS_PATH");

                if (credentialsPathFromEnv != null && !credentialsPathFromEnv.isEmpty()) {
                    System.out.println("Found GOOGLE_CREDENTIALS_PATH environment variable. Path: " + credentialsPathFromEnv);
                    File credentialsFile = new File(credentialsPathFromEnv);
                    if (credentialsFile.exists()) {
                        // Môi trường Production (Render) hoặc Local được cấu hình chuyên nghiệp
                        serviceAccountStream = new FileInputStream(credentialsFile);
                        System.out.println("Successfully loaded credentials from environment variable path.");
                    } else {
                        System.err.println("WARNING: Environment variable GOOGLE_CREDENTIALS_PATH was set, but file not found at: " + credentialsPathFromEnv);
                    }
                }
                
                // 2. Nếu không có biến môi trường hoặc file không tồn tại, quay về phương án dự phòng
                if (serviceAccountStream == null) {
                    System.out.println("Falling back to reading credentials from /WEB-INF/ (for simple local development).");
                    
                    ServletContext context = sce.getServletContext();
                    String localResourcePath = "/WEB-INF/" + CREDENTIALS_FILE_NAME;
                    serviceAccountStream = context.getResourceAsStream(localResourcePath);

                    if (serviceAccountStream == null) {
                        System.err.println("CRITICAL ERROR: Could not find credentials file via environment variable or in /WEB-INF/ directory.");
                        throw new IOException("Cannot find " + CREDENTIALS_FILE_NAME + ".");
                    }
                    System.out.println("Successfully loaded credentials from /WEB-INF/.");
                }

                // === KẾT THÚC PHẦN TỐI ƯU HÓA ===

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
    public void contextDestroyed(ServletContextEvent sce) { }
}
