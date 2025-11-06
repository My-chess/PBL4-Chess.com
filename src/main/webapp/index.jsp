<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%-- Đảm bảo bạn có dòng này để lấy contextPath --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<html>
<head>
    <title>Sảnh Chờ - Cờ Tướng Online</title>
    <link rel="stylesheet" href="${contextPath}/css/style.css">
    <link rel="stylesheet" href="${contextPath}/css/game.css"> <%-- Thêm game.css để dùng chung styles --%>
</head>
<body>
    <jsp:include page="/WEB-INF/views/common/header.jsp" />

    <div class="container lobby-container">
        <h1>Tìm trận đấu</h1>
        <hr>
        <%-- Phần này giữ nguyên như lobby.jsp hiện có (tạo/tham gia phòng người vs người) --%>
        <div class="lobby-panel create-game">
            <h2>Bắt đầu ván cờ mới (Người vs Người)</h2>
            <p>Tạo một phòng chơi và mời bạn bè cùng tham gia!</p>
            <button id="open-create-game-modal" class="btn">Tạo Ván Mới</button>
            <a href="${contextPath}/lobby" class="btn btn-secondary">Tìm phòng</a>
        </div>

        <div class="lobby-panel join-game">
            <h2>Tham gia ván cờ (Người vs Người)</h2>
            <form action="${contextPath}/joinGame" method="POST" class="join-form">
                <div class="form-group" style="margin:0; flex-grow:1;">
                    <input type="text" name="gameId" placeholder="Nhập mã phòng..." required>
                </div>
                <button type="submit" class="btn">Vào Phòng</button>
            </form>
        </div>

        <%-- THÊM PHẦN CHƠI VỚI MÁY MỚI --%>
        <hr style="margin-top: 40px;">
        <div class="lobby-panel play-ai-game" style="text-align: center;">
            <h2>Chơi với Máy (AI)</h2>
            <p>Thử thách kỹ năng của bạn với AI!</p>
            <button id="open-ai-game-modal" class="btn btn-secondary">Chơi với Máy</button>
        </div>
        <%-- KẾT THÚC PHẦN CHƠI VỚI MÁY MỚI --%>

        <c:if test="${not empty param.error or not empty errorMessage}">
            <div class="error-message">${errorMessage}</div>
        </c:if>
    </div>

    <%-- Modal cài đặt trận đấu (Người vs Người) - Giữ nguyên như của bạn --%>
    <div id="create-game-modal" class="modal-overlay">
        <div class="modal-content">
            <button id="close-modal-btn" class="modal-close-btn">&times;</button>
            <h2>Cài đặt Ván đấu (Người vs Người)</h2>
            <form action="${contextPath}/startGame" method="POST" class="modal-form">
                <div class="form-group">
                    <label for="time-control">Thời gian mỗi bên</label>
                    <select id="time-control" name="timeControl">
                        <option value="5">5 phút</option>
                        <option value="10" selected>10 phút</option>
                        <option value="15">15 phút</option>
                        <option value="30">30 phút</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="side-preference">Chọn màu quân</label>
                    <select id="side-preference" name="sidePreference">
                        <option value="Random">Ngẫu nhiên</option>
                        <option value="Red">Quân Đỏ</option>
                        <option value="Black">Quân Đen</option>
                    </select>
                </div>
                <div class="form-group">
                    <div class="checkbox-group">
                        <input type="checkbox" id="is-ranked" name="isRanked" value="true" checked>
                        <label for="is-ranked" style="margin-bottom: 0;">Trận đấu xếp hạng (có tính ELO)</label>
                    </div>
                </div>
                <button type="submit" class="btn" style="width: 100%;">Bắt đầu</button>
            </form>
        </div>
    </div>

    <%-- MODAL CÀI ĐẶT TRẬN ĐẤU VỚI MÁY MỚI --%>
    <div id="create-ai-game-modal" class="modal-overlay">
        <div class="modal-content">
            <button id="close-ai-modal-btn" class="modal-close-btn">&times;</button>
            <h2>Cài đặt Ván đấu (Người vs Máy)</h2>
            <form action="${contextPath}/ai/newGame" method="POST" class="modal-form">
                <div class="form-group">
                    <label for="ai-side-preference">Chọn màu quân của bạn</label>
                    <select id="ai-side-preference" name="playerColor">
                        <option value="Red" selected>Quân Đỏ (Bạn đi trước)</option>
                        <option value="Black">Quân Đen (Máy đi trước)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="ai-difficulty">Chọn độ khó của Máy</label>
                    <select id="ai-difficulty" name="difficulty">
                        <option value="Easy">Dễ</option>
                        <option value="Medium" selected>Trung bình</option>
                        <option value="Hard">Khó</option>
                    </select>
                </div>
                <button type="submit" class="btn" style="width: 100%;">Bắt đầu</button>
            </form>
        </div>
    </div>
    <%-- KẾT THÚC MODAL CÀI ĐẶT TRẬN ĐẤU VỚI MÁY MỚI --%>


    <%-- JAVASCRIPT ĐIỀU KHIỂN MODALs (Thêm logic cho modal AI) --%>
    <script>
        const contextPath = "${contextPath}";

        // Logic cho modal người vs người (giữ nguyên)
        const openModalBtn = document.getElementById('open-create-game-modal');
        const closeModalBtn = document.getElementById('close-modal-btn');
        const modal = document.getElementById('create-game-modal');
        openModalBtn.addEventListener('click', () => { modal.style.display = 'flex'; });
        closeModalBtn.addEventListener('click', () => { modal.style.display = 'none'; });

        // Logic cho modal người vs máy (MỚI)
        const openAiModalBtn = document.getElementById('open-ai-game-modal');
        const closeAiModalBtn = document.getElementById('close-ai-modal-btn');
        const aiModal = document.getElementById('create-ai-game-modal');
        openAiModalBtn.addEventListener('click', () => { aiModal.style.display = 'flex'; });
        closeAiModalBtn.addEventListener('click', () => { aiModal.style.display = 'none'; });

        // Đóng modals khi click ra ngoài
        window.addEventListener('click', (event) => {
            if (event.target === modal) { modal.style.display = 'none'; }
            if (event.target === aiModal) { aiModal.style.display = 'none'; }
        });
    </script>
</body>
</html>