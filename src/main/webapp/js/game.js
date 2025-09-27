document.addEventListener('DOMContentLoaded', () => {
    // --- CẤU HÌNH FIREBASE ---
    // Cấu hình bạn đã cung cấp, đảm bảo nó là chính xác.
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

    firebase.initializeApp(firebaseConfig);
    const db = firebase.firestore();
    console.log("✅ 1. Firebase Initialized Successfully.");

    // --- LẤY CÁC THÀNH PHẦN UI ---
    const boardElement = document.getElementById('board');
    const turnColorEl = document.getElementById('turn-color');
    const statusMessageEl = document.getElementById('status-message');
    const moveListEl = document.getElementById('move-list');

    // --- BIẾN TOÀN CỤC ---
    let selectedSquare = null;
    let websocket = null;
    let isSpectator = false;
    let myColor = null; // Sẽ lưu màu của người chơi hiện tại ('Red' hoặc 'Black')
    let currentTurnFromServer = null;
    let timerInterval = null;

    console.log(`✅ 2. Game ID: "${gameId}", User ID: "${currentUserId}"`);

    if (!gameId) {
        statusMessageEl.textContent = "❌ Lỗi: Không tìm thấy ID ván cờ trong URL.";
        return;
    }

    // --- CÁC HÀM XỬ LÝ ---

    function setupBoard() {
        boardElement.innerHTML = '';
        for (let y = 0; y < 10; y++) {
            for (let x = 0; x < 9; x++) {
                const square = document.createElement('div');
                square.classList.add('square');
                square.dataset.x = x;
                square.dataset.y = y;
                boardElement.appendChild(square);
            }
        }

        const initialPositions = {
            "0,0": "RookBEAN_Black", "1,0": "KnightBEAN_Black", "2,0": "BishopBEAN_Black", "3,0": "AdvisorBEAN_Black", "4,0": "KingBEAN_Black", "5,0": "AdvisorBEAN_Black", "6,0": "BishopBEAN_Black", "7,0": "KnightBEAN_Black", "8,0": "RookBEAN_Black",
            "1,2": "CannonBEAN_Black", "7,2": "CannonBEAN_Black",
            "0,3": "PawnBEAN_Black", "2,3": "PawnBEAN_Black", "4,3": "PawnBEAN_Black", "6,3": "PawnBEAN_Black", "8,3": "PawnBEAN_Black",
            "0,9": "RookBEAN_Red", "1,9": "KnightBEAN_Red", "2,9": "BishopBEAN_Red", "3,9": "AdvisorBEAN_Red", "4,9": "KingBEAN_Red", "5,9": "AdvisorBEAN_Red", "6,9": "BishopBEAN_Red", "7,9": "KnightBEAN_Red", "8,9": "RookBEAN_Red",
            "1,7": "CannonBEAN_Red", "7,7": "CannonBEAN_Red",
            "0,6": "PawnBEAN_Red", "2,6": "PawnBEAN_Red", "4,6": "PawnBEAN_Red", "6,6": "PawnBEAN_Red", "8,6": "PawnBEAN_Red"
        };

        Object.entries(initialPositions).forEach(([pos, pieceName]) => {
            const [x, y] = pos.split(',');
            const square = document.querySelector(`.square[data-x='${x}'][data-y='${y}']`);
            if (square) {
                square.innerHTML = `<img src="${contextPath}/images/${pieceName}.png" alt="${pieceName}">`;
            }
        });
    }

    function applyMoves(moveDocs) {
        moveDocs.forEach(doc => {
            const move = doc.data();
            const startSquare = document.querySelector(`.square[data-x='${move.startX}'][data-y='${move.startY}']`);
            const endSquare = document.querySelector(`.square[data-x='${move.endX}'][data-y='${move.endY}']`);
            if (startSquare && endSquare && startSquare.innerHTML) {
                endSquare.innerHTML = startSquare.innerHTML;
                startSquare.innerHTML = '';
            }
        });
    }

    function updateAndStartTimer(matchData) {
        clearInterval(timerInterval);
        if (!matchData.lastMoveTimestamp) return;

        const serverNow = firebase.firestore.Timestamp.now().toMillis();
        const lastMoveTime = matchData.lastMoveTimestamp.toMillis();
        const timeElapsed = matchData.status === 'IN_PROGRESS' ? serverNow - lastMoveTime : 0;

        let p1Time = matchData.player1TimeLeftMs;
        let p2Time = matchData.player2TimeLeftMs;

        if (matchData.currentTurn === 'Red') p1Time -= timeElapsed;
        else p2Time -= timeElapsed;

        timerInterval = setInterval(() => {
            if (matchData.status !== 'IN_PROGRESS') {
                clearInterval(timerInterval);
                return;
            }

            const p1Display = document.getElementById('timer-red');
            const p2Display = document.getElementById('timer-black');

            p1Display.textContent = formatTime(p1Time);
            p2Display.textContent = formatTime(p2Time);

            if (matchData.currentTurn === 'Red') {
                p1Time -= 1000;
                p1Display.classList.add('active');
                p2Display.classList.remove('active');
                if (p1Time <= 0 && myColor === 'Red') {
                    clearInterval(timerInterval);
                    if (websocket && websocket.readyState === WebSocket.OPEN) {
                        websocket.send(JSON.stringify({ action: 'timeout' }));
                    }
                }
            } else {
                p2Time -= 1000;
                p2Display.classList.add('active');
                p1Display.classList.remove('active');

                if (p2Time <= 0 && myColor === 'Black') {
                    clearInterval(timerInterval);
                    if (websocket && websocket.readyState === WebSocket.OPEN) {
                        websocket.send(JSON.stringify({ action: 'timeout' }));
                    }
                }
            }
        }, 1000);
    }

    function formatTime(ms) {
        if (ms <= 0) return "00:00";
        const totalSeconds = Math.floor(ms / 1000);
        const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
        const seconds = (totalSeconds % 60).toString().padStart(2, '0');
        return `${minutes}:${seconds}`;
    }

    function connectWebSocket() {
        const isSecure = window.location.protocol === 'https:';
        const protocol = isSecure ? 'wss://' : 'ws://';

        // URL này sẽ tự động đúng cho cả localhost (ws) và Render (wss)
        const wsUrl = `${protocol}${window.location.host}/game/${gameId}`;

        console.log(`Attempting to connect to WebSocket at: ${wsUrl}`);
        websocket = new WebSocket(wsUrl);

        websocket.onopen = () => {
            console.log("✅ WebSocket Connection Established!");
            statusMessageEl.textContent = "Sẵn sàng chơi!";
        };

        websocket.onclose = (event) => {
            console.warn("WebSocket Connection Closed.", event);
            statusMessageEl.textContent = "Mất kết nối server.";
        };
        websocket.onerror = (err) => console.error("❌ WebSocket Error:", err);
    }

    const matchDocRef = db.collection('matches').doc(gameId);
    matchDocRef.onSnapshot(doc => {
        console.log("✅ 4. Match listener triggered. UI will be updated.");
        if (!doc.exists) { /* ... */ return; }

        const matchData = doc.data();
        currentTurnFromServer = matchData.currentTurn;
        const p1 = matchData.player1;
        const p2 = matchData.player2;

        // Cập nhật tên và ELO
        document.getElementById('player-red-name').textContent = p1 ? p1.displayName : "Chờ người chơi...";
        document.getElementById('player-black-name').textContent = p2 ? p2.displayName : "Chờ người chơi...";
        document.getElementById('player-red-elo').textContent = (p1 && p1.elo) ? `ELO: ${p1.elo}` : "";
        document.getElementById('player-black-elo').textContent = (p2 && p2.elo) ? `ELO: ${p2.elo}` : "";

        // Xác định vai trò
        if (p1 && p1.uid === currentUserId) { myColor = "Red"; isSpectator = false; }
        else if (p2 && p2.uid === currentUserId) { myColor = "Black"; isSpectator = false; }
        else { myColor = null; isSpectator = true; }

        // Cập nhật trạng thái
        if (matchData.currentTurn) {
            turnColorEl.textContent = matchData.currentTurn;
            turnColorEl.className = matchData.currentTurn.toLowerCase();
            if (!isSpectator) {
                statusMessageEl.textContent = (matchData.currentTurn === myColor) ? "Đến lượt bạn!" : "Đang chờ đối thủ...";
            } else {
                statusMessageEl.textContent = `Đến lượt quân ${matchData.currentTurn}...`;
            }
        }
        updateAndStartTimer(matchData);
    }, error => console.error("❌ Firestore Error on Match listener: ", error));

    const movesCollectionRef = matchDocRef.collection('moves').orderBy('timestamp');
    let isInitialLoad = true;
    movesCollectionRef.onSnapshot(snapshot => {
        console.log("✅ 5. Moves listener triggered.");
        if (isInitialLoad) {
            setupBoard();
            isInitialLoad = false;
        }
        applyMoves(snapshot.docs);
        // (Code cập nhật move list giữ nguyên)
    }, error => console.error("❌ Firestore Error on Moves listener: ", error));

    // --- XỬ LÝ CLICK TRÊN BÀN CỜ (ĐÃ SỬA LỖI VÀ CẢI TIẾN) ---
    boardElement.addEventListener('click', (event) => {
        if (isSpectator || currentTurnFromServer !== myColor) {
            statusMessageEl.textContent = isSpectator ? "Khán giả không thể đi cờ." : "Chưa đến lượt của bạn.";
            return;
        }

        const clickedSquare = event.target.closest('.square');
        if (!clickedSquare) return;

        const clickedPieceImg = clickedSquare.querySelector('img');

        // --- LẦN CLICK THỨ HAI (ĐI CỜ hoặc ĐỔI QUÂN CHỌN) ---
        if (selectedSquare) {
            // A. Nếu click vào một quân cờ khác CÙNG MÀU -> Đổi lựa chọn
            if (clickedPieceImg && clickedPieceImg.src.includes(`_${myColor}`)) {
                selectedSquare.classList.remove('selected');
                selectedSquare = clickedSquare;
                selectedSquare.classList.add('selected');
                return;
            }

            // B. Nếu click vào ô khác (để đi cờ)
            const startX = selectedSquare.dataset.x;
            const startY = selectedSquare.dataset.y;
            const endX = clickedSquare.dataset.x;
            const endY = clickedSquare.dataset.y;

            if (websocket && websocket.readyState === WebSocket.OPEN) {
                const moveData = { "startX": parseFloat(startX), "startY": parseFloat(startY), "endX": parseFloat(endX), "endY": parseFloat(endY) };
                websocket.send(JSON.stringify(moveData));
                statusMessageEl.textContent = "Đã gửi nước đi...";
            } else {
                statusMessageEl.textContent = "Mất kết nối server.";
            }
            selectedSquare.classList.remove('selected');
            selectedSquare = null;

            // --- LẦN CLICK ĐẦU TIÊN (CHỌN QUÂN) ---
        } else if (clickedPieceImg) {
            const pieceColor = clickedPieceImg.src.includes('_Red') ? 'Red' : 'Black';
            if (pieceColor === myColor) {
                selectedSquare = clickedSquare;
                selectedSquare.classList.add('selected');
            } else {
                statusMessageEl.textContent = "Bạn không thể điều khiển quân của đối thủ.";
            }
        }
    });

    // --- KHỞI CHẠY ---
    connectWebSocket();
});
