# --- GIAI ĐOẠN 1: BUILD ---
# Sử dụng một image có sẵn cả Maven và Java 17 để làm "môi trường build"
# Đặt tên cho giai đoạn này là "builder"
FROM maven:3.9-eclipse-temurin-17 AS builder

# Thiết lập thư mục làm việc bên trong container
WORKDIR /app

# Sao chép file pom.xml trước để tận dụng Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Sao chép toàn bộ mã nguồn của dự án vào
COPY src ./src

# Chạy lệnh Maven để build dự án. 
# -DskipTests để bỏ qua việc chạy test, giúp build nhanh hơn.
RUN mvn package -DskipTests

# --- GIAI ĐOẠN 2: FINAL ---
# Bắt đầu lại với một image Tomcat gọn nhẹ cho môi trường chạy thực tế
FROM tomcat:10.1-jdk17-temurin

# Xóa các ứng dụng web mặc định của Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file .war đã được tạo ra ở Giai đoạn 1 ("builder")
# vào thư mục webapps của Tomcat và đổi tên thành ROOT.war
COPY --from=builder /app/target/PBL4-Chess.war /usr/local/tomcat/webapps/ROOT.war

# Cổng mặc định của Tomcat
EXPOSE 8080

# Lệnh để khởi động Tomcat
CMD ["catalina.sh", "run"]
