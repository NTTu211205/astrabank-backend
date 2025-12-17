//package org.astrabank.config;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.*;
//
//@Configuration
//public class FirebaseConfig {
//    @Bean
//    public FirebaseApp firebaseApp() throws IOException {
//        // Đọc file JSON từ thư mục resources
//        FileInputStream serviceAccount =
//                new FileInputStream("src/main/resources/serviceAccountKey.json");
//
//        FirebaseOptions options = FirebaseOptions.builder()
//                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                .build();
//
//        // Kiểm tra để tránh khởi tạo lại nếu đã tồn tại
//        if (FirebaseApp.getApps().isEmpty()) {
//            return FirebaseApp.initializeApp(options);
//        }
//        return FirebaseApp.getInstance();
//
//        // Lấy nội dung JSON từ biến môi trường FIREBASE_CONFIG
//        String firebaseConfig = System.getenv("FIREBASE_CONFIG");
//
//// Kiểm tra xem có lấy được không (đề phòng quên set trên Render)
//        if (firebaseConfig == null || firebaseConfig.isEmpty()) {
//            // Fallback: Nếu chạy dưới máy local mà chưa set biến môi trường thì thử đọc file cũ
//            // (Giúp bạn vẫn chạy được dưới máy tính của mình mà không cần setup phức tạp)
//            try {
//                InputStream serviceAccount = new FileInputStream("src/main/resources/ServiceAccountKey.json");
//                FirebaseOptions options = new FirebaseOptions.Builder()
//                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                        .build();
//                FirebaseApp.initializeApp(options);
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException("Không tìm thấy Firebase Config (File hoặc Env Var)");
//            }
//        } else {
//            // Nếu có biến môi trường (trên Render) thì dùng cái này
//            InputStream serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes());
//            FirebaseOptions options = new FirebaseOptions.Builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .build();
//            FirebaseApp.initializeApp(options);
//        }
//    }
//}


package org.astrabank.config;// Nhớ đổi package cho đúng với dự án của bạn

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration // Đánh dấu đây là class cấu hình Spring Boot
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 1. Kiểm tra xem App đã được khởi tạo chưa để tránh lỗi "FirebaseApp name [DEFAULT] already exists"
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        InputStream serviceAccountStream = null;

        // 2. Ưu tiên lấy từ Biến môi trường (Dành cho Render/Production)
        String firebaseConfigEnv = System.getenv("FIREBASE_CONFIG");

        if (firebaseConfigEnv != null && !firebaseConfigEnv.isEmpty()) {
            // Nếu có biến môi trường -> Chuyển String thành Stream
            serviceAccountStream = new ByteArrayInputStream(firebaseConfigEnv.getBytes());
        } else {
            // 3. Nếu không có biến môi trường -> Tìm file ở máy local (Dành cho Dev)
            File file = new File("src/main/resources/ServiceAccountKey.json");
            if (file.exists()) {
                serviceAccountStream = new FileInputStream(file);
            }
        }

        // 4. Nếu cả 2 cách đều không tìm thấy dữ liệu -> Báo lỗi dừng chương trình ngay
        if (serviceAccountStream == null) {
            throw new IllegalStateException("LỖI: Không tìm thấy cấu hình Firebase! " +
                    "Hãy kiểm tra biến môi trường FIREBASE_CONFIG (trên Server) " +
                    "hoặc file src/main/resources/ServiceAccountKey.json (ở Local).");
        }

        // 5. Khởi tạo Firebase với Stream đã lấy được
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}