package Controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebListener
public class FirebaseConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("FirebaseConfig: Initializing Firebase Admin SDK...");
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Lấy nội dung key từ BIẾN MÔI TRƯỜNG
                String firebaseCredentials = System.getenv("GOOGLE_CREDENTIALS_JSON");

                if (firebaseCredentials == null || firebaseCredentials.isEmpty()) {
                    throw new RuntimeException("Environment variable GOOGLE_CREDENTIALS_JSON is not set.");
                }

                // Chuyển chuỗi JSON thành InputStream
                InputStream serviceAccount = new ByteArrayInputStream(firebaseCredentials.getBytes(StandardCharsets.UTF_8));
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK has been initialized successfully from env variable.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Firebase Admin SDK initialization failed.", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) { }
}