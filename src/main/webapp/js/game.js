/**
 * File: game.js
 * Chịu trách nhiệm cho toàn bộ logic phía client của trang game.
 * Bao gồm:
 * - Khởi tạo Firebase và kết nối WebSocket.
 * - Lắng nghe và hiển thị trạng thái game (bàn cờ, người chơi, thời gian) từ Firestore.
 * - Xử lý tương tác của người dùng trên bàn cờ (chọn, di chuyển quân).
 * - Gửi các hành động của người dùng (nước đi, hết giờ) lên server.
 * - Hiển thị các thông báo từ server (lỗi, kết thúc game).
 */
document.addEventListener('DOMContentLoaded', () => {
    // --- PHẦN 1: CẤU HÌNH VÀ KHỞI TẠO ---

    // Cấu hình Firebase (đảm bảo các key này là chính xác)
    const firebaseConfig = {
        apiKey: "AIzaSyBB2GREXzftCQkV41Rlwka1SPBulY1NVhI",
        authDomain: "pbl4-chess-fdcdb.firebaseapp.com",
        databaseURL: "https://pbl4-chess-fdcdb-default-rtdb.asia-southeast1.firebasedatabase.app",
        projectId: "pbl4-chess-fdcdb",
        storageBucket: "pbl4-chess-fdcdb.appspot.com",
        messagingSenderId: "1046253329967",
        appId: "1:1046253329967:web:d130010d7516699f0970da",
        measurementId: "G-0ZJN640VB7"
    };

    // Khởi tạo Firebase
    firebase.initializeApp(firebaseConfig);
    const db = firebase.firestore();
    console.log("✅ 1. Firebase Initialized Successfully.");

    // Lấy các thành phần UI từ HTML để tương tác
    const boardElement = document.getElementById('board');
    const turnColorEl = document.getElementById('turn-color');
    const statusMessageEl = document.getElementById('status-message');
    const moveListEl = document.getElementById('move-list'); // Giữ lại nếu bạn muốn hiển thị lịch sử nước đi
	const gameOverModal = document.getElementById('gameOverModal');
	   const gameOverMessageEl = document.getElementById('gameOverMessage');

    // Biến toàn cục để quản lý trạng thái của client
    let selectedSquare = null;      // Ô cờ đang được người dùng chọn
    let websocket = null;           // Đối tượng WebSocket
    let myColor = null;             // Màu quân của người chơi hiện tại ('Red' hoặc 'Black')
    let isSpectator = false;         // Mặc định là khán giả cho đến khi xác định được vai trò
    let currentTurnFromServer = null; // Lưu lại lượt đi hiện tại từ server
    let timerInterval = null;       // Biến để quản lý bộ đếm thời gian

    console.log(`✅ 2. Game ID: "${gameId}", User ID: "${currentUserId}"`);

    // Kiểm tra xem gameId có tồn tại không (được truyền từ JSP)
    if (!gameId) {
        statusMessageEl.textContent = "❌ Lỗi: Không tìm thấy ID ván cờ trong URL.";
        return;
    }


    // --- PHẦN 2: CÁC HÀM XỬ LÝ CHÍNH ---

    /**
     * >>> HÀM QUAN TRỌNG NHẤT <<<
     * Vẽ lại toàn bộ bàn cờ từ một đối tượng trạng thái nhận từ Firestore.
     * Hàm này đảm bảo giao diện luôn đồng bộ với dữ liệu trên server.
     * @param {object} boardState - Đối tượng có dạng {"y,x": "PieceType_Color"}
     */
    function renderBoardFromState(boardState) {
        boardElement.innerHTML = ''; // Xóa sạch bàn cờ cũ để vẽ lại từ đầu

        // Lặp qua 10 hàng và 9 cột để tạo 90 ô cờ
        for (let y = 0; y < 10; y++) {
            for (let x = 0; x < 9; x++) {
                const square = document.createElement('div');
                square.classList.add('square');
                square.dataset.x = x; // Lưu tọa độ x, y vào data-attributes
                square.dataset.y = y;
                
                // Kiểm tra xem tại ô [y,x] có quân cờ nào trong boardState không
                const pieceKey = `${y},${x}`;
                if (boardState && boardState[pieceKey]) {
                    const pieceName = boardState[pieceKey]; // Ví dụ: "RookBEAN_Black"
                    // Chèn hình ảnh quân cờ vào ô
                    square.innerHTML = `<img src="${contextPath}/images/${pieceName}.png" alt="${pieceName}">`;
                }
                
                boardElement.appendChild(square);
            }
        }
    }

    /**
     * Cập nhật và bắt đầu bộ đếm thời gian cho cả hai người chơi.
     * @param {object} matchData - Dữ liệu đầy đủ của trận đấu từ Firestore.
     */
    function updateAndStartTimer(matchData) {
        clearInterval(timerInterval); // Dừng bộ đếm cũ trước khi bắt đầu cái mới
        if (!matchData.lastMoveTimestamp) return;

        // Tính toán thời gian đã trôi qua kể từ nước đi cuối cùng
        const serverNow = firebase.firestore.Timestamp.now().toMillis();
        const lastMoveTime = matchData.lastMoveTimestamp.toMillis();
        const timeElapsed = matchData.status === 'IN_PROGRESS' ? serverNow - lastMoveTime : 0;

        let p1Time = matchData.player1TimeLeftMs;
        let p2Time = matchData.player2TimeLeftMs;

        // Trừ thời gian đã trôi qua cho người chơi đang trong lượt
        if (matchData.currentTurn === 'Red') {
            p1Time -= timeElapsed;
        } else {
            p2Time -= timeElapsed;
        }

        // Bắt đầu bộ đếm ngược mỗi giây
        timerInterval = setInterval(() => {
            if (matchData.status !== 'IN_PROGRESS') {
                clearInterval(timerInterval);
                return;
            }

            const p1Display = document.getElementById('timer-red');
            const p2Display = document.getElementById('timer-black');

            // Cập nhật hiển thị thời gian
            p1Display.textContent = formatTime(p1Time);
            p2Display.textContent = formatTime(p2Time);
            
            // Xác định người chơi nào đang trong lượt để trừ thời gian
            if (matchData.currentTurn === 'Red') {
                p1Time -= 1000;
                p1Display.classList.add('active'); // Làm nổi bật đồng hồ của người đang đi
                p2Display.classList.remove('active');

                // Nếu hết giờ và là lượt của mình, gửi thông báo timeout lên server
                if (p1Time < 0 && myColor === 'Red') {
                    clearInterval(timerInterval);
                    if (websocket && websocket.readyState === WebSocket.OPEN) {
                        websocket.send(JSON.stringify({ type: 'timeout' }));
                    }
                }
            } else {
                p2Time -= 1000;
                p2Display.classList.add('active');
                p1Display.classList.remove('active');

                if (p2Time < 0 && myColor === 'Black') {
                     clearInterval(timerInterval);
                    if (websocket && websocket.readyState === WebSocket.OPEN) {
                        websocket.send(JSON.stringify({ type: 'timeout' }));
                    }
                }
            }
        }, 1000);
    }

    /**
     * Định dạng thời gian từ mili giây thành chuỗi "mm:ss".
     * @param {number} ms - Thời gian tính bằng mili giây.
     * @returns {string} Chuỗi thời gian đã định dạng.
     */
    function formatTime(ms) {
        if (ms <= 0) return "00:00";
        const totalSeconds = Math.floor(ms / 1000);
        const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
        const seconds = (totalSeconds % 60).toString().padStart(2, '0');
        return `${minutes}:${seconds}`;
    }

    /**
     * Kết nối tới server WebSocket.
     */
    function connectWebSocket() {
        // Tự động chọn wss:// cho HTTPS và ws:// cho HTTP
        const isSecure = window.location.protocol === 'https:';
        const protocol = isSecure ? 'wss://' : 'ws://';
        const wsUrl = `${protocol}${window.location.host}${contextPath}/game/${gameId}`;

        console.log(`✅ 3. Attempting to connect to WebSocket at: ${wsUrl}`);
        websocket = new WebSocket(wsUrl);

        websocket.onopen = () => {
            console.log("✅ WebSocket Connection Established!");
            statusMessageEl.textContent = "Sẵn sàng chơi!";
        };

        websocket.onclose = (event) => {
            console.warn("WebSocket Connection Closed.", event);
            statusMessageEl.textContent = "Mất kết nối server.";
            clearInterval(timerInterval); // Dừng đồng hồ khi mất kết nối
        };
        
        websocket.onerror = (err) => console.error("❌ WebSocket Error:", err);
        
        // >>> Xử lý tin nhắn nhận được từ Server <<<
        websocket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log("Received message from server:", data);

            if (data.type === 'ERROR') {
                // Hiển thị lỗi cho người dùng khi server từ chối một hành động
                statusMessageEl.textContent = `Lỗi: ${data.message}`;
                
                // Tạo hiệu ứng lắc cho ô cờ đã chọn để báo hiệu nước đi sai
                if (selectedSquare) {
                    selectedSquare.classList.add('error-shake');
                    setTimeout(() => {
                        selectedSquare.classList.remove('error-shake');
                        // Reset lại thông báo trạng thái sau khi hiển thị lỗi
                        statusMessageEl.textContent = (currentTurnFromServer === myColor) ? "Đến lượt bạn!" : "Đang chờ đối thủ...";
                    }, 600);
                }
            } else if (data.type === 'GAME_OVER') {
                // Xử lý khi nhận được thông báo kết thúc game
                clearInterval(timerInterval);
				const winnerText = data.winnerColor === 'Red' ? "Phe Đỏ" : "Phe Đen";
				                let reasonText = '';
				                if (data.reason === 'CHECKMATE') {
				                    reasonText = 'do chiếu bí';
				                } else if (data.reason === 'TIMEOUT') {
				                    reasonText = 'do đối thủ hết giờ';
				                } else if (data.reason === 'RESIGN') {
				                    reasonText = 'do đối thủ đầu hàng';
				                }

				                // 3. Tạo thông báo kết quả đầy đủ
				                const finalMessage = `${winnerText} đã giành chiến thắng ${reasonText}!`;
				                
				                // 4. Cập nhật nội dung và hiển thị modal
				                gameOverMessageEl.textContent = finalMessage;
				                gameOverModal.style.display = 'flex'; // << Kích hoạt modal
				                
				                // Cập nhật cả thanh trạng thái chính
				                statusMessageEl.textContent = "Ván cờ đã kết thúc.";
            }
        };
    }


    // --- PHẦN 3: LẮNG NGHE SỰ KIỆN ---

    // Lắng nghe sự thay đổi trên document của trận đấu trong Firestore
    const matchDocRef = db.collection('matches').doc(gameId);
    matchDocRef.onSnapshot(doc => {
        console.log("✅ 4. Match listener triggered. UI will be updated.");
        if (!doc.exists) {
            statusMessageEl.textContent = "Lỗi: Ván cờ này không còn tồn tại.";
            return;
        }

        const matchData = doc.data();
        currentTurnFromServer = matchData.currentTurn;

        // 1. Vẽ lại bàn cờ với trạng thái mới nhất từ `boardState`
        if (matchData.boardState) {
            renderBoardFromState(matchData.boardState);
        }

        // 2. Cập nhật thông tin người chơi và ELO
        const p1 = matchData.player1;
        const p2 = matchData.player2;
        document.getElementById('player-red-name').textContent = p1 ? p1.displayName : "Chờ người chơi...";
        document.getElementById('player-black-name').textContent = p2 ? p2.displayName : "Chờ người chơi...";
        document.getElementById('player-red-elo').textContent = (p1 && p1.elo) ? `ELO: ${p1.elo}` : "";
        document.getElementById('player-black-elo').textContent = (p2 && p2.elo) ? `ELO: ${p2.elo}` : "";

        // 3. Xác định vai trò của người dùng (người chơi hay khán giả)
        if (p1 && p1.uid === currentUserId) { myColor = "Red"; isSpectator = false; }
        else if (p2 && p2.uid === currentUserId) { myColor = "Black"; isSpectator = false; }
        else { myColor = null; isSpectator = true; }

        // 4. Cập nhật thông báo trạng thái và lượt đi
        if (matchData.status === 'IN_PROGRESS') {
            turnColorEl.textContent = matchData.currentTurn;
            turnColorEl.className = matchData.currentTurn.toLowerCase(); // 'Red' -> 'red'
            if (!isSpectator) {
                statusMessageEl.textContent = (matchData.currentTurn === myColor) ? "Đến lượt bạn!" : "Đang chờ đối thủ...";
            } else {
                statusMessageEl.textContent = `Đang xem: Lượt của quân ${matchData.currentTurn}`;
            }
        } else if (matchData.status === 'WAITING') {
            statusMessageEl.textContent = `Đang chờ người chơi khác... Mã phòng: ${gameId}`;
        }
        
        // 5. Cập nhật và khởi động lại đồng hồ đếm ngược
        updateAndStartTimer(matchData);

    }, error => console.error("❌ Firestore Error on Match listener: ", error));

    // Lắng nghe sự kiện click trên bàn cờ
    boardElement.addEventListener('click', (event) => {
        // Ngăn chặn hành động nếu là khán giả, chưa đến lượt hoặc game chưa bắt đầu
        if (isSpectator || currentTurnFromServer !== myColor) {
            const message = isSpectator ? "Bạn là khán giả, không thể đi cờ." : "Chưa đến lượt của bạn.";
            statusMessageEl.textContent = message;
            return;
        }

        const clickedSquare = event.target.closest('.square');
        if (!clickedSquare) return; // Click ra ngoài ô cờ

        const clickedPieceImg = clickedSquare.querySelector('img');

        if (selectedSquare) {
            // --- Lần click thứ 2: Thực hiện nước đi hoặc đổi quân chọn ---

            // Nếu click vào một quân cờ khác cùng màu -> Đổi lựa chọn
            if (clickedPieceImg && clickedPieceImg.src.includes(`_${myColor}`)) {
                selectedSquare.classList.remove('selected');
                selectedSquare = clickedSquare;
                selectedSquare.classList.add('selected');
                return;
            }

            // Nếu click vào ô khác để thực hiện nước đi
            const startX = selectedSquare.dataset.x;
            const startY = selectedSquare.dataset.y;
            const endX = clickedSquare.dataset.x;
            const endY = clickedSquare.dataset.y;

            if (websocket && websocket.readyState === WebSocket.OPEN) {
                // Đóng gói dữ liệu nước đi vào một đối tượng chuẩn có 'type'
                const message = {
                    type: "move",
                    data: { 
                        "startX": parseFloat(startX), 
                        "startY": parseFloat(startY), 
                        "endX": parseFloat(endX), 
                        "endY": parseFloat(endY) 
                    }
                };
                websocket.send(JSON.stringify(message));
                statusMessageEl.textContent = "Đã gửi nước đi, chờ xác nhận...";
            } else {
                statusMessageEl.textContent = "Lỗi: Mất kết nối tới server.";
            }
            
            // Bỏ chọn ô cờ sau khi gửi nước đi
            selectedSquare.classList.remove('selected');
            selectedSquare = null;

        } else if (clickedPieceImg) {
            // --- Lần click đầu tiên: Chọn một quân cờ ---

            // Kiểm tra xem quân cờ được click có phải là của mình không
            const pieceColor = clickedPieceImg.src.includes('_Red') ? 'Red' : 'Black';
            if (pieceColor === myColor) {
                selectedSquare = clickedSquare;
                selectedSquare.classList.add('selected'); // Thêm class để highlight
            } else {
                statusMessageEl.textContent = "Bạn không thể điều khiển quân của đối thủ.";
            }
        }
    });

    // --- PHẦN 4: KHỞI CHẠY ---
    connectWebSocket();
});