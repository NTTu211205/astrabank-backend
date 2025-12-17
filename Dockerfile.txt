# Bước 1: Build dự án
FROM maven:3.8.5-openjdk-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Chạy ứng dụng
FROM openjdk:21.0.1-jdk-slim
COPY --from=build /target/*.jar app.jar
# Copy file key "giả" để tránh lỗi khởi động (sẽ ghi đè bằng biến môi trường sau)
COPY src/main/resources/ServiceAccountKey.json /app/src/main/resources/ServiceAccountKey.json
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]