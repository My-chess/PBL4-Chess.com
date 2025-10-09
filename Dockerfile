# ----- GIAI ĐOẠN 1: BUILD ỨNG DỤNG BẰNG MAVEN -----
# Sử dụng một image có sẵn Maven và Java 17 để biên dịch và đóng gói
FROM maven:3.8-openjdk-17 AS build

# Đặt thư mục làm việc bên trong container là /app
WORKDIR /app

# Sao chép file pom.xml vào trước.
# Đây là một mẹo để tận dụng cache của Docker, giúp build nhanh hơn ở các lần sau.
COPY pom.xml .

# Sao chép toàn bộ mã nguồn của bạn vào
COPY src ./src

# Chạy lệnh Maven để build dự án.
# Kết quả sẽ là một file ROOT.war trong thư mục /app/target/
RUN mvn clean package -DskipTests


# ----- GIAI ĐOẠN 2: CHẠY ỨNG DỤNG TRÊN TOMCAT -----
# Sử dụng một image Tomcat 10.1 chính thức, nhẹ và đã được tối ưu
FROM tomcat:10.1-jdk17-temurin

# Xóa các ứng dụng web mặc định của Tomcat để giữ image gọn nhẹ
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file ROOT.war đã được build ở Giai đoạn 1 vào thư mục webapps của Tomcat
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Mở cổng 8080 để Render có thể kết nối vào
EXPOSE 8080

# Lệnh để khởi động Tomcat khi container chạy.
CMD ["catalina.sh", "run"]
