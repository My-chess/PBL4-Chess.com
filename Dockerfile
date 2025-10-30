# --- Giai đoạn 1: Build ứng dụng với Maven ---
# Sử dụng một image có sẵn Maven và JDK 11
FROM maven:3.8-openjdk-11 AS build

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép file pom.xml vào trước để tận dụng cache của Docker
COPY pom.xml .

# Tải tất cả dependencies về trước
RUN mvn dependency:go-offline

# Sao chép toàn bộ mã nguồn còn lại
COPY src ./src

# Chạy lệnh build của Maven. Nó sẽ biên dịch code và đóng gói thành file .war
# Lệnh 'package -DskipTests' sẽ bỏ qua việc chạy unit test để build nhanh hơn
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Chạy ứng dụng trên Tomcat ---
# Sử dụng image Tomcat 10.1
FROM tomcat:10.1-jdk11-openjdk

# Dọn dẹp các ứng dụng mặc định
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file .war đã được build từ giai đoạn 1 vào thư mục webapps của Tomcat
# Maven sẽ tạo ra file .war trong thư mục /target
COPY --from=build /app/target/cotuong-webapp.war /usr/local/tomcat/webapps/ROOT.war

# Mở cổng 8080
EXPOSE 8080

# Lệnh khởi động Tomcat
CMD ["catalina.sh", "run"]
