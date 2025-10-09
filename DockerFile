# Bắt đầu với một image Tomcat 10.1 và Java 17
FROM tomcat:10.1-jdk17-temurin

# Chỉ định thư mục làm việc bên trong container
WORKDIR /usr/local/tomcat

# Xóa các ứng dụng web mặc định của Tomcat để dọn dẹp
RUN rm -rf webapps/*

# Sao chép file pom.xml để tải các thư viện cần thiết
COPY pom.xml .
# Chạy Maven để tải các dependencies (tận dụng Docker cache)
RUN mvn dependency:go-offline

# Sao chép toàn bộ mã nguồn của dự án vào container
COPY src ./src

# Chạy Maven để build dự án thành file .war bên trong container
# Lệnh này sẽ tạo ra file /usr/local/tomcat/target/PBL4-Chess.war
RUN mvn package

# Sao chép file .war đã build vào thư mục webapps và đổi tên thành ROOT.war
# để ứng dụng chạy ở thư mục gốc (ví dụ: yoursite.com/)
RUN cp target/PBL4-Chess.war webapps/ROOT.war

# Cổng mặc định của Tomcat
EXPOSE 8080

# Lệnh để khởi động Tomcat khi container chạy
CMD ["catalina.sh", "run"]
