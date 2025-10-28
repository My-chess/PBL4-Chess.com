<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<html>
<head>
    <title>Đăng Nhập - Cờ Tướng Online</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="container">
        <h1>Đăng Nhập</h1>
        
        <c:if test="${param.success == 'true'}">
            <div class="success-message">
                Đăng ký thành công! Vui lòng đăng nhập.
            </div>
        </c:if>

        <%-- 
            THAY ĐỔI QUAN TRỌNG: 
            - Bỏ 'action' và 'method' vì JavaScript sẽ xử lý việc gửi form.
            - Thêm một 'id' để JavaScript có thể dễ dàng chọn form này.
        --%>
        <form id="login-form">
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required>
            </div>
            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <input type="password" id="password" name="password" required>
            </div>
            <button type="submit" class="btn">Đăng Nhập</button>
        </form>
        
        <%-- Vẫn giữ lại phần hiển thị lỗi từ server (nếu có) --%>
        <div id="client-error-message" class="error-message" style="display: none;"></div>
        <c:if test="${not empty errorMessage}">
            <div class="error-message">${errorMessage}</div>
        </c:if>

        <div class="form-link">
            Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register.jsp">Tạo tài khoản</a>
        </div>
    </div>

    <!-- BƯỚC 1: THÊM FIREBASE JAVASCRIPT SDK -->
    <!-- Luôn đặt các script này ở cuối thẻ <body> -->
    <script src="https://www.gstatic.com/firebasejs/8.10.1/firebase-app.js"></script>
    <script src="https://www.gstatic.com/firebasejs/8.10.1/firebase-auth.js"></script>

    <script>
        // BƯỚC 2: CẤU HÌNH FIREBASE VỚI THÔNG TIN CỦA BẠN
        // Lấy các thông tin này từ project Firebase của bạn
        const firebaseConfig = {
            apiKey: "AIzaSyBB2GREXzftCQkV41Rlwka1SPBulY1NVhI", // Thay bằng API Key của bạn
            authDomain: "pbl4-chess-fdcdb.firebaseapp.com", // Thay bằng Auth Domain của bạn
            projectId: "pbl4-chess-fdcdb", // Thay bằng Project ID của bạn
            // ... các thông tin khác nếu có
        };

        // Khởi tạo Firebase
        firebase.initializeApp(firebaseConfig);
        const auth = firebase.auth();

        // BƯỚC 3: XỬ LÝ SỰ KIỆN SUBMIT FORM BẰNG JAVASCRIPT
        const loginForm = document.getElementById('login-form');
        const clientErrorMessage = document.getElementById('client-error-message');

        loginForm.addEventListener('submit', (event) => {
            // Ngăn chặn hành vi mặc định của form (tải lại trang)
            event.preventDefault();

            // Lấy email và password từ input
            const email = loginForm.email.value;
            const password = loginForm.password.value;

            // Gọi hàm đăng nhập của Firebase
            auth.signInWithEmailAndPassword(email, password)
                .then((userCredential) => {
                    // Đăng nhập thành công!
                    console.log('Firebase login successful:', userCredential.user);
                    
                    // Lấy idToken từ user
                    return userCredential.user.getIdToken();
                })
                .then((idToken) => {
                    // Đã có idToken, bây giờ gửi nó lên server của bạn
                    console.log('Got ID Token. Submitting to backend server...');
                    createAndSubmitTokenForm(idToken);
                })
                .catch((error) => {
                    // Xử lý lỗi đăng nhập từ Firebase
                    console.error('Firebase login error:', error);
                    let message = "Email hoặc mật khẩu không chính xác.";
                    if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
                        message = "Email hoặc mật khẩu không chính xác.";
                    } else if (error.code === 'auth/invalid-email') {
                        message = "Định dạng email không hợp lệ.";
                    }
                    // Hiển thị lỗi cho người dùng
                    clientErrorMessage.textContent = message;
                    clientErrorMessage.style.display = 'block';
                });
        });

        /**
         * Hàm này tạo một form ẩn, đặt idToken vào đó và submit lên servlet /login
         * @param {string} token - The idToken from Firebase.
         */
        function createAndSubmitTokenForm(token) {
            // Tạo một element form ảo
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '${pageContext.request.contextPath}/login'; // Action trỏ đến LoginServlet

            // Tạo một input ẩn để chứa token
            const hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = 'idToken'; // Tên phải khớp với request.getParameter("idToken") ở server
            hiddenInput.value = token;

            // Thêm input vào form và thêm form vào trang
            form.appendChild(hiddenInput);
            document.body.appendChild(form);

            // Gửi form đi
            form.submit();
        }
    </script>
</body>
</html>