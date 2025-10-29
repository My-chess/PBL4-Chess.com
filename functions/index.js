// functions/index.js

// Import các thư viện cần thiết của Firebase Admin SDK
const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Khởi tạo ứng dụng Firebase Admin với quyền truy cập đầy đủ
admin.initializeApp();

// Lấy tham chiếu đến Firestore và Realtime Database
const firestore = admin.firestore();
const database = admin.database();

/**
 * ==================================================================
 * PHẦN 1: NGƯỜI GÁC CỔNG REAL-TIME (Hệ thống Hiện diện)
 * ==================================================================
 * Cloud Function này được kích hoạt TỰ ĐỘNG mỗi khi có bất kỳ sự thay đổi nào
 * đối với dữ liệu tại đường dẫn '/status/{uid}' trong Realtime Database.
 *
 * Nó hoạt động dựa trên cơ chế "di chúc" (onDisconnect) mà client đã đăng ký.
 */
exports.onUserStatusChanged = functions.database.ref("/status/{uid}").onUpdate(
    async (change, context) => {
        // Lấy dữ liệu trạng thái MỚI của người dùng sau khi thay đổi
        const eventStatus = change.after.val();

        // Lấy UID của người dùng từ tham số trên đường dẫn (phần {uid})
        const uid = context.params.uid;

        // Lấy tham chiếu đến chính node dữ liệu này để có thể ghi lại nếu cần
        const userStatusFirestoreRef = firestore.doc(`status/${uid}`);

        // Nếu dữ liệu mới là 'offline' (isOnline === false)
        // Điều này có nghĩa là người dùng vừa mới ngắt kết nối.
        if (eventStatus.isOnline === false) {
            functions.logger.log(`Người dùng ${uid} đã offline. Đang tìm kiếm các phòng chờ cần dọn dẹp.`);

            // Truy vấn Firestore để tìm TẤT CẢ các phòng mà người này đã tạo
            // VÀ hiện vẫn đang ở trạng thái 'WAITING' (chờ người chơi).
            const waitingMatchesQuery = firestore.collection("matches")
                .where("creatorId", "==", uid)
                .where("status", "==", "WAITING");
            
            // Thực hiện truy vấn
            const snapshot = await waitingMatchesQuery.get();

            // Nếu không tìm thấy phòng nào thỏa mãn điều kiện, kết thúc function.
            if (snapshot.empty) {
                functions.logger.log(`Không tìm thấy phòng chờ nào của người dùng ${uid}.`);
                return null;
            }

            // Nếu tìm thấy, chúng ta sẽ xóa tất cả chúng cùng một lúc bằng batch.
            const batch = firestore.batch();
            snapshot.forEach(doc => {
                functions.logger.log(`Đang xóa phòng chờ bị bỏ rơi: ${doc.id} (tạo bởi ${uid}).`);
                // Thêm lệnh xóa document này vào batch
                batch.delete(doc.ref);
            });

            // Thực thi batch để xóa tất cả các document đã chọn.
            await batch.commit();
            functions.logger.log(`Đã dọn dẹp xong các phòng chờ của người dùng ${uid}.`);
        }

        // (Tùy chọn) Bạn có thể muốn đồng bộ trạng thái online/offline này 
        // sang Firestore để các client khác dễ dàng truy vấn hơn.
        // Nếu không cần tính năng "danh sách bạn bè online", bạn có thể bỏ dòng này.
        return userStatusFirestoreRef.set(eventStatus);
    }
);

/**
 * ==================================================================
 * PHẦN 2: NGƯỜI GÁC ĐÊM (Dọn dẹp theo lịch trình - Tùy chọn)
 * ==================================================================
 * Cloud Function này chạy định kỳ mỗi 5 phút.
 * Nhiệm vụ của nó là tìm và xóa các phòng vẫn còn 'WAITING' nhưng đã quá hạn
 * (ví dụ: tạo cách đây hơn 15 phút) mà vì lý do nào đó chưa bị xóa.
 * 
 * Để dùng được hàm này, bạn cần bật Cloud Scheduler API trong Google Cloud Console.
 * Nếu thấy phức tạp, bạn có thể tạm thời comment lại phần này.
 */
/*
exports.cleanupExpiredMatches = functions.pubsub.schedule('every 5 minutes').onRun(async (context) => {
    // Định nghĩa thời gian hết hạn là 15 phút trước
    const TIMEOUT_MINUTES = 15;
    const now = admin.firestore.Timestamp.now();
    // Tính toán mốc thời gian "cắt": hiện tại trừ đi 15 phút
    const cutoffTime = new admin.firestore.Timestamp(now.seconds - (TIMEOUT_MINUTES * 60), now.nanoseconds);

    // Truy vấn các phòng 'WAITING' có 'startTime' trước mốc thời gian cắt
    const expiredMatchesQuery = firestore.collection('matches')
        .where('status', '==', 'WAITING')
        .where('startTime', '<', cutoffTime);

    const snapshot = await expiredMatchesQuery.get();

    if (snapshot.empty) {
        functions.logger.log("Không có phòng chờ quá hạn nào cần dọn dẹp.");
        return null;
    }

    // Xóa tất cả các phòng quá hạn bằng batch
    const batch = firestore.batch();
    snapshot.forEach(doc => {
        functions.logger.log(`Đang xóa phòng chờ quá hạn: ${doc.id} (tạo lúc: ${doc.data().startTime.toDate()})`);
        batch.delete(doc.ref);
    });

    await batch.commit();
    functions.logger.log(`Đã dọn dẹp thành công ${snapshot.size} phòng chờ quá hạn.`);
    return null;
});
*/