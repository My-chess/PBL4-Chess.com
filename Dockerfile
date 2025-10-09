# ----- GIAI ĐOẠN 1: BUILD ỨNG DỤNG BẰNG MAVEN -----
# Sử dụng một image có sẵn Maven và Java 11
FROM maven:3.8-openjdk-11 AS build

# Đặt thư mục làm việc bên trong container là /app
WORKDIR /app

# Sao chép file pom.xml và toàn bộ mã nguồn vào
COPY pom.xml .
COPY src ./src

# === THAY ĐỔI QUAN TRỌNG NHẤT LÀ Ở ĐÂY ===
# Chạy một lệnh duy nhất để tải dependencies và build dự án.
# Lệnh này sẽ tự động tải mọi thứ cần thiết trước khi biên dịch.
RUN mvn clean package -DskipTests


# ----- GIAI ĐOẠN 2: CHẠY ỨNG DỤNG TRÊN TOMCAT -----
# Sử dụng một image Tomcat 10.1 chạy trên Java 11
FROM tomcat:10.1-jdk11-temurin

# Xóa các ứng dụng web mặc định của Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file ROOT.war đã được build ở Giai đoạn 1 vào thư mục webapps của Tomcat
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Mở cổng 8080
EXPOSE 8080

# Lệnh để khởi động Tomcat
CMD ["catalina.sh", "run"]
