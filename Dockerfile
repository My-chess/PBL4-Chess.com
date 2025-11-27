# ==========================================================
# GIAI ĐOẠN 1: BUILD ỨNG DỤNG BẰNG MAVEN ("THE BUILDER")
# ==========================================================
# Sử dụng một image có sẵn Maven và Java 11 để biên dịch và đóng gói.
# Image này chỉ tồn tại tạm thời để tạo ra file .war.
FROM maven:3.8-openjdk-11 AS build

# Đặt thư mục làm việc bên trong container là /app
WORKDIR /app

# Sao chép file pom.xml vào trước.
# Đây là một mẹo để tận dụng cache của Docker. Bước tải thư viện bên dưới
# sẽ chỉ chạy lại khi file pom.xml này có sự thay đổi.
COPY pom.xml .

# Chạy lệnh Maven để tải tất cả các thư viện cần thiết về.
RUN mvn dependency:go-offline

# Bây giờ mới sao chép toàn bộ mã nguồn của bạn vào.
COPY src ./src

# Chạy lệnh Maven để build toàn bộ dự án (biên dịch code và đóng gói).
# Lệnh này sẽ sử dụng các thư viện đã được tải về từ bước trước.
# Kết quả sẽ là một file ROOT.war trong thư mục /app/target/
RUN mvn clean package -DskipTests


# ==========================================================
# GIAI ĐOẠN 2: CHẠY ỨNG DỤNG TRÊN TOMCAT ("THE RUNNER")
# ==========================================================
# Sử dụng một image Tomcat 10.1 chính thức, nhẹ và đã được tối ưu, chạy trên Java 11.
# Đây sẽ là image cuối cùng được triển khai.
FROM tomcat:10.1-jdk11-temurin

# Xóa các ứng dụng web mặc định của Tomcat (manager, examples,...) để giữ image gọn nhẹ và an toàn.
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file ROOT.war đã được build ở Giai đoạn 1 vào thư mục webapps của Tomcat.
# Cú pháp --from=build sẽ lấy kết quả từ giai đoạn "build" ở trên.
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Mở cổng 8080 để Render (hoặc các dịch vụ khác) có thể kết nối vào.
EXPOSE 8080

# Lệnh để khởi động Tomcat khi container chạy.
# 'run' sẽ giữ cho Tomcat chạy ở foreground, điều này là bắt buộc đối với Docker.
CMD ["catalina.sh", "run"]
