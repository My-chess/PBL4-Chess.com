<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%
    if (session.getAttribute("loggedInUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return; 
    }
%>
<html>
<head>
    <title>Sảnh Chờ - Cờ Tướng Online</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    
    <%-- CSS CHO MODAL CÀI ĐẶT TRẬN ĐẤU --%>
    <style>
        .modal-overlay {
            position: fixed;
            top: 0; left: 0;
            width: 100%; height: 100%;
            background-color: rgba(0, 0, 0, 0.7);
            display: none; /* Ẩn mặc định */
            justify-content: center;
            align-items: center;
            z-index: 2000;
        }
        .modal-content {
            background-color: #2c2c2e;
            padding: 30px;
            border-radius: 8px;
            width: 90%;
            max-width: 450px;
            box-shadow: 0 5px 15px rgba(0,0,0,0.5);
            position: relative;
        }
        .modal-close-btn {
            position: absolute;
            top: 10px; right: 15px;
            font-size: 28px;
            color: #aaa;
            cursor: pointer;
            border: none; background: none;
        }
        .modal-content h2 {
            margin-top: 0;
            margin-bottom: 25px;
            text-align: center;
        }
        .modal-form .form-group {
            margin-bottom: 20px;
        }
        .modal-form label {
            display: block;
            margin-bottom: 8px;
            color: #ccc;
        }
        .modal-form select, .modal-form .checkbox-group {
            width: 100%;
            padding: 10px;
            background-color: #1c1c1e;
            border: 1px solid #444;
            color: white;
            border-radius: 5px;
            font-size: 16px;
        }
        .checkbox-group {
            display: flex;
            align-items: center;
        }
        .checkbox-group input {
            margin-right: 10px;
            width: 20px; height: 20px;
        }
    </style>
</head>
<body>
    <jsp:include page="WEB-INF/views/common/header.jsp" />

    <div class="container lobby-container">
        <div class="lobby-panel create-game">
            <h2>Bắt đầu ván cờ mới</h2>
            <p>Tạo một phòng chơi và mời bạn bè cùng tham gia!</p>
            <%-- Sửa lại nút này để mở modal --%>
            <button id="open-create-game-modal" class="btn">Tạo Ván Mới</button>
            <a href="${pageContext.request.contextPath}/lobby" class="btn btn-secondary">Tìm phòng</a>
        </div>

        <div class="lobby-panel join-game">
            <h2>Tham gia ván cờ</h2>
            <form action="${pageContext.request.contextPath}/joinGame" method="POST" class="join-form">
                <div class="form-group" style="margin:0; flex-grow:1;">
                    <input type="text" name="gameId" placeholder="Nhập mã phòng..." required>
                </div>
                <button type="submit" class="btn">Vào Phòng</button>
            </form>
        </div>
        
        <c:if test="${not empty param.error or not empty errorMessage}">
             <div class="error-message">${errorMessage}</div>
        </c:if>
    </div>

    <%-- MODAL CÀI ĐẶT TRẬN ĐẤU (ẨN MẶC ĐỊNH) --%>
    <div id="create-game-modal" class="modal-overlay">
        <div class="modal-content">
            <button id="close-modal-btn" class="modal-close-btn">&times;</button>
            <h2>Cài đặt Ván đấu</h2>
            <form action="${pageContext.request.contextPath}/startGame" method="POST" class="modal-form">
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

    <%-- JAVASCRIPT ĐỂ ĐIỀU KHIỂN MODAL --%>
    <script>
        const openModalBtn = document.getElementById('open-create-game-modal');
        const closeModalBtn = document.getElementById('close-modal-btn');
        const modal = document.getElementById('create-game-modal');

        openModalBtn.addEventListener('click', () => {
            modal.style.display = 'flex';
        });

        closeModalBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });

        // Đóng modal khi click ra ngoài
        window.addEventListener('click', (event) => {
            if (event.target === modal) {
                modal.style.display = 'none';
            }
        });
    </script>

</body>
</html>