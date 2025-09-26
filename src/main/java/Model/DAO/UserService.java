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

    /**
     * Tạo một người dùng mới trên Firestore khi họ đăng ký.
     */
    public static UserBEAN createUser(String email, String username)
            throws ExecutionException, InterruptedException {
        DocumentReference docRef = getDb().collection("users").document();
        String newUid = docRef.getId();
        UserBEAN newUser = new UserBEAN(newUid, username, email, INITIAL_ELO, 0, 0, 0);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", newUid);
        userMap.put("email", email);
        userMap.put("username", username);
        userMap.put("elo", INITIAL_ELO);
        userMap.put("winCount", 0);
        userMap.put("loseCount", 0);
        userMap.put("drawCount", 0);

        docRef.set(userMap).get();// Dùng getDb()
        return newUser;
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
     */
    public static void updateUserStatsAfterMatch(String winnerId, String loserId, boolean isDraw)
            throws ExecutionException, InterruptedException {
        WriteBatch batch = getDb().batch();

        DocumentSnapshot winnerDoc = getDb().collection("users").document(winnerId).get().get();
        DocumentSnapshot loserDoc = getDb().collection("users").document(loserId).get().get();

        if (!winnerDoc.exists() || !loserDoc.exists()) {
            System.err.println("User not found for stat update.");
            return;
        }

        if (isDraw) {
            // Cập nhật trận hòa cho cả hai
            batch.update(winnerDoc.getReference(), "drawCount", winnerDoc.getLong("drawCount") + 1);
            batch.update(loserDoc.getReference(), "drawCount", loserDoc.getLong("drawCount") + 1);
        } else {
            // Cập nhật thắng/thua và ELO
            long winnerOldElo = winnerDoc.getLong("elo");
            long loserOldElo = loserDoc.getLong("elo");

            batch.update(winnerDoc.getReference(), "winCount", winnerDoc.getLong("winCount") + 1);
            batch.update(winnerDoc.getReference(), "elo", winnerOldElo + ELO_CHANGE_AMOUNT);

            batch.update(loserDoc.getReference(), "loseCount", loserDoc.getLong("loseCount") + 1);
            batch.update(loserDoc.getReference(), "elo", Math.max(0, loserOldElo - ELO_CHANGE_AMOUNT)); // Đảm bảo ELO
                                                                                                        // không âm
        }

        batch.commit().get();
    }
}
