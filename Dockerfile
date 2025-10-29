# Sử dụng image Tomcat làm nền tảng DUY NHẤT cho cả build và run
FROM tomcat:9.0-jdk11-openjdk

# Đặt thư mục làm việc tạm thời để build code
WORKDIR /build_app

# Sao chép TOÀN BỘ dự án của bạn vào thư mục này
COPY . .

# LỆNH BIÊN DỊCH ĐÃ SỬA ĐỔI - ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT
# -cp (classpath): Bao gồm 2 phần, được ngăn cách bởi dấu hai chấm (:)
#   1. "/usr/local/tomcat/lib/*": Thêm TẤT CẢ các thư viện có sẵn của Tomcat vào classpath.
#   2. "src/main/webapp/WEB-INF/lib/*": Thêm các thư viện bạn đã cung cấp trong thư mục lib.
RUN javac -d src/main/webapp/WEB-INF/classes -cp "/usr/local/tomcat/lib/*:src/main/webapp/WEB-INF/lib/*" $(find src/main/java -name "*.java")

# --- Giai đoạn thiết lập để chạy server ---

# Xóa các ứng dụng web mặc định của Tomcat để dọn dẹp
RUN rm -rf /usr/local/tomcat/webapps/*

# Di chuyển ứng dụng đã được biên dịch của bạn vào đúng vị trí (thư mục ROOT)
RUN mv src/main/webapp /usr/local/tomcat/webapps/ROOT

# (Tùy chọn) Dọn dẹp thư mục build tạm thời để giảm kích thước image
RUN rm -rf /build_app

# Chuyển về thư mục làm việc mặc định của Tomcat
WORKDIR /usr/local/tomcat

# Mở cổng 8080 mà Tomcat lắng nghe
EXPOSE 8080
# Lệnh để khởi động server Tomcat
CMD ["catalina.sh", "run"]
