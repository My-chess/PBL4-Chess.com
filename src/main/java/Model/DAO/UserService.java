package Model.DAO;

import Model.BEAN.UserBEAN;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class UserService {

    // private static final Firestore db = FirestoreClient.getFirestore();
    private static final int INITIAL_ELO = 1000;
    private static final int ELO_CHANGE_AMOUNT = 15; // Tăng/giảm 15 ELO mỗi trận

    private static Firestore getDb() {
        return FirestoreClient.getFirestore();
    }

 // Trong lớp UserService.java

    /**
     * Tạo một người dùng mới trên Firestore SAU KHI đã tạo trên Firebase Auth.
     * @param uid UID trả về từ Firebase Authentication.
     */
    public static void createUser(String uid, String email, String username)
            throws ExecutionException, InterruptedException {
        // Bây giờ, ID của document chính là uid từ Firebase Auth
        DocumentReference docRef = getDb().collection("users").document(uid); 
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid); // Quan trọng: lưu uid
        userMap.put("email", email);
        userMap.put("username", username);
        userMap.put("elo", INITIAL_ELO);
        userMap.put("winCount", 0);
        userMap.put("loseCount", 0);
        userMap.put("drawCount", 0);

        docRef.set(userMap).get();
    }

    /**
     * Lấy thông tin người dùng từ Firestore.
     */
    public static UserBEAN getUser(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getDb().collection("users").document(uid).get().get(); // Dùng getDb()
        if (doc.exists()) {
            return doc.toObject(UserBEAN.class);
        }
        return null;
    }

    public static UserBEAN getUserByEmail(String email) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = getDb().collection("users") // Dùng getDb()
                .whereEqualTo("email", email)
                .limit(1)
                .get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (!documents.isEmpty()) {
            return documents.get(0).toObject(UserBEAN.class);
        }
        return null;
    }

    /**
     * Cập nhật chỉ số và ELO cho người chơi sau khi trận đấu kết thúc.
     * @param id1 ID người chơi thứ nhất
     * @param id2 ID người chơi thứ hai
     * @param isDraw Nếu là true, id1 và id2 đều được tính hòa. Nếu false, id1 được coi là người thắng, id2 là người thua.
     */
    public static void updateUserStatsAfterMatch(String matchId, String id1, String id2, boolean isDraw)
            throws ExecutionException, InterruptedException {
        
        // Bước 1: Lấy thông tin trận đấu để kiểm tra có phải xếp hạng không
        DocumentSnapshot matchDoc = FirebaseService.getMatch(matchId);
        if (!matchDoc.exists()) {
            System.err.println("Match not found for stat update.");
            return;
        }
        boolean isRanked = matchDoc.getBoolean("isRanked") != null && matchDoc.getBoolean("isRanked");

        // Bước 2: Lấy thông tin người chơi
        WriteBatch batch = getDb().batch();
        DocumentReference p1Ref = getDb().collection("users").document(id1);
        DocumentReference p2Ref = getDb().collection("users").document(id2);
        DocumentSnapshot p1Doc = p1Ref.get().get();
        DocumentSnapshot p2Doc = p2Ref.get().get();
        if (!p1Doc.exists() || !p2Doc.exists()) return;

        // Bước 3: Cập nhật chỉ số
        if (isDraw) {
            batch.update(p1Ref, "drawCount", p1Doc.getLong("drawCount") + 1);
            batch.update(p2Ref, "drawCount", p2Doc.getLong("drawCount") + 1);
            // ELO không thay đổi khi hòa trong hệ thống đơn giản này
        } else {
            // id1 là người thắng, id2 là người thua
            DocumentReference winnerRef = p1Ref;
            DocumentReference loserRef = p2Ref;
            DocumentSnapshot winnerDoc = p1Doc;
            DocumentSnapshot loserDoc = p2Doc;

            batch.update(winnerRef, "winCount", winnerDoc.getLong("winCount") + 1);
            batch.update(loserRef, "loseCount", loserDoc.getLong("loseCount") + 1);

            // CHỈ CẬP NHẬT ELO NẾU LÀ TRẬN XẾP HẠNG
            if (isRanked) {
                long winnerOldElo = winnerDoc.getLong("elo");
                long loserOldElo = loserDoc.getLong("elo");
                batch.update(winnerRef, "elo", winnerOldElo + ELO_CHANGE_AMOUNT);
                batch.update(loserRef, "elo", Math.max(0, loserOldElo - ELO_CHANGE_AMOUNT));
            }
        }

        batch.commit().get();
    }
    public static String getUserDisplayName(String userId) 
            throws ExecutionException, InterruptedException {
        
        if (userId == null || userId.isEmpty()) {
            return "Khách"; // Tên mặc định nếu userId rỗng
        }

        // 1. Tham chiếu đến document của người dùng trong collection "users"
        DocumentReference userRef = getDb().collection("users").document(userId);
        
        // 2. Lấy document
        ApiFuture<DocumentSnapshot> future = userRef.get();
        DocumentSnapshot document = future.get();
        
        // 3. Xử lý kết quả
        if (document.exists()) {
            String displayName = document.getString("username");
            
            // Nếu tìm thấy, trả về displayName
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        }
        
        // 4. Fallback: Nếu document không tồn tại, HOẶC không có trường displayName
        // Trả về chính userId làm tên hiển thị
        System.err.println("Không tìm thấy displayName cho user: " + userId + ". Sử dụng tạm userId.");
        return userId;
    }
}
