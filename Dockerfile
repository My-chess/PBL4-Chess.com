# --- Giai đoạn 1: Build ứng dụng với Maven ---
# Sử dụng Maven mới nhất với JDK 11 (Temurin chính chủ)
FROM maven:3.9.9-eclipse-temurin-11 AS build

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép file pom.xml trước (tận dụng cache)
COPY pom.xml .

# Tải dependencies để build offline sau này
RUN mvn -B dependency:go-offline

# Sao chép toàn bộ mã nguồn
COPY src ./src

# Build project, bỏ qua test để nhanh hơn
RUN mvn -B package -DskipTests

# --- Giai đoạn 2: Chạy ứng dụng trên Tomcat ---
FROM tomcat:10.1-jdk11

# Xóa các ứng dụng mặc định của Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file WAR từ stage build
# Pom.xml của bạn đã đặt <finalName>ROOT</finalName>, nên file sẽ là ROOT.war
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Mở cổng 8080
EXPOSE 8080

# Lệnh khởi động Tomcat
CMD ["catalina.sh", "run"]
