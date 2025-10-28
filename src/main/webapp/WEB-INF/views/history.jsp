<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %> <%-- Dòng quan trọng để định dạng ngày tháng --%>

<html>
<head>
    <title>Lịch Sử Đấu - ${loggedInUser.username}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    
    <%-- CSS dành riêng cho trang lịch sử đấu --%>
    <style>
        .history-container {
            max-width: 900px; /* Giới hạn chiều rộng để dễ đọc trên màn hình lớn */
            margin: 40px auto;
            padding: 20px;
        }

        .history-table {
            width: 100%;
            border-collapse: collapse;
            color: #f0f0f0;
            margin-top: 25px;
            font-size: 16px;
        }

        .history-table th, .history-table td {
            padding: 15px;
            text-align: left;
            border-bottom: 1px solid #3e3e3e;
        }

        .history-table th {
            background-color: #3a3a3c;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #aaa;
        }
        
        /* Làm cho toàn bộ hàng có thể click và có hiệu ứng hover */
        .history-table tbody tr {
            cursor: pointer;
            transition: background-color 0.2s ease-in-out;
        }

        .history-table tbody tr:hover {
            background-color: #3f3f41;
        }

        /* "Huy hiệu" cho kết quả */
        .result-badge {
            padding: 5px 12px;
            border-radius: 15px;
            font-weight: bold;
            font-size: 14px;
            color: white;
            display: inline-block;
        }

        .result-win { background-color: #4CAF50; } /* Xanh lá */
        .result-loss { background-color: #f44336; } /* Đỏ */
        .result-draw { background-color: #777; } /* Xám */

        .elo-change.win { color: #4CAF50; }
        .elo-change.loss { color: #f44336; }
        
        .win-reason {
            font-style: italic;
            color: #bbb;
        }

        /* Nút xem lại (tùy chọn) */
        .btn-replay {
            padding: 8px 16px;
            background-color: #007bff;
            color: white;
            border-radius: 5px;
            text-decoration: none;
            font-weight: bold;
            transition: background-color 0.2s;
        }
        .btn-replay:hover {
            background-color: #0056b3;
        }

    </style>
</head>
<body>
    <%-- Nhúng header chung --%>
    <jsp:include page="/WEB-INF/views/common/header.jsp" />
    
    <div class="container history-container">
        <h1>Lịch Sử Đấu</h1>
        
        <table class="history-table">
            <thead>
                <tr>
                    <th>Đối thủ</th>
                    <th>Kết quả</th>
                    <th>Lý do kết thúc</th>
                    <th>Thay đổi ELO</th>
                    <th>Ngày</th>
                </tr>
            </thead>
            <tbody>
                <c:if test="${empty matchHistory}">
                    <tr>
                        <td colspan="5" style="text-align: center; padding: 30px;">Bạn chưa có trận đấu nào.</td>
                    </tr>
                </c:if>

                <c:forEach items="${matchHistory}" var="match">
                    <%-- Xác định đối thủ --%>
                    <c:set var="opponent" value="${match.player1.uid == loggedInUser.uid ? match.player2 : match.player1}" />
                    
                    <%-- Xác định kết quả và các class CSS tương ứng --%>
                    <c:choose>
                        <c:when test="${match.winnerId == loggedInUser.uid}">
                            <c:set var="resultClass" value="result-win" />
                            <c:set var="resultText" value="Thắng" />
                            <c:set var="eloClass" value="win" />
                            <c:set var="eloChange" value="+15" />
                        </c:when>
                        <c:when test="${not empty match.winnerId}">
                            <c:set var="resultClass" value="result-loss" />
                            <c:set var="resultText" value="Thua" />
                            <c:set var="eloClass" value="loss" />
                            <c:set var="eloChange" value="-15" />
                        </c:when>
                        <c:otherwise>
                            <c:set var="resultClass" value="result-draw" />
                            <c:set var="resultText" value="Hòa" />
                            <c:set var="eloClass" value="" />
                            <c:set var="eloChange" value="0" />
                        </c:otherwise>
                    </c:choose>
                    
                    <%-- Dùng JavaScript để làm cho cả hàng có thể click được --%>
                    <tr onclick="window.location='<c:url value='/app/replay?matchId=${match.matchId}'/>'">
                        <td>
                            <strong>${opponent.username}</strong>
                            <div style="font-size: 14px; color: #aaa;">ELO: ${opponent.elo}</div>
                        </td>
                        <td>
                            <span class="result-badge ${resultClass}">${resultText}</span>
                        </td>
                        <td class="win-reason">
                            <c:choose>
                               <%-- Các lý do Thắng/Thua --%>
        <c:when test="${match.winReason == 'CHECKMATE'}">Chiếu bí</c:when>
        <c:when test="${match.winReason == 'TIMEOUT'}">Hết giờ</c:when>
        <c:when test="${match.winReason == 'RESIGN'}">Đầu hàng</c:when>
        <c:when test="${match.winReason == 'DISCONNECT'}">Mất kết nối</c:when>
        
        <%-- THÊM CÁC LÝ DO HÒA VÀO ĐÂY --%>
        <c:when test="${match.winReason == 'DRAW_AGREEMENT'}">Thỏa thuận hòa</c:when>
        <c:when test="${match.winReason == 'DRAW_REPETITION'}">Lặp lại nước đi</c:when>
        
        <%-- Trường hợp mặc định nếu không có lý do --%>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="elo-change ${eloClass}">
                            <strong>${eloChange}</strong>
                        </td>
                        <td>
                            <%-- Định dạng ngày tháng cho dễ đọc --%>
                            <fmt:formatDate value="${match.startTime.toDate()}" pattern="dd/MM/yyyy HH:mm" />
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</body>
</html>