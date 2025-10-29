# Bước 1: Môi trường Build - Sử dụng một image có sẵn Java Development Kit (JDK)
FROM openjdk:11-jdk AS build
# Đặt thư mục làm việc bên trong container
WORKDIR /app
# Sao chép TOÀN BỘ dự án của bạn vào container
COPY . .

# Lệnh quan trọng: Biên dịch code Java
# -d: Thư mục đích cho các file .class (đây là vị trí tiêu chuẩn)
# -cp: Classpath - บอก cho trình biên dịch biết nơi tìm các file .jar dependency
# $(find...): Tự động tìm tất cả các file có đuôi .java trong thư mục src/main/java để biên dịch
RUN javac -d src/main/webapp/WEB-INF/classes -cp "src/main/webapp/WEB-INF/lib/*" $(find src/main/java -name "*.java")

# Bước 2: Môi trường Chạy - Sử dụng một image Tomcat chính thức
FROM tomcat:9.0-jdk11-openjdk
# Xóa các ứng dụng web mặc định của Tomcat để dọn dẹp
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép toàn bộ ứng dụng web đã được biên dịch từ Bước 1
# Điều này bao gồm JSP, CSS, JS, thư mục lib, và thư mục classes MỚI được tạo ra
# Chúng ta sao chép nó vào thư mục webapps/ROOT để nó trở thành ứng dụng mặc định
COPY --from=build /app/src/main/webapp /usr/local/tomcat/webapps/ROOT

# Mở cổng 8080 mà Tomcat lắng nghe
EXPOSE 8080
# Lệnh để khởi động server Tomcat
CMD ["catalina.sh", "run"]