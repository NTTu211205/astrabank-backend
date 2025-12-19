package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.iid.FirebaseInstanceId;
import org.astrabank.dto.ChangePINRequest;
import org.astrabank.dto.UpdateUserRequest;
import org.astrabank.models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user) throws ExecutionException, InterruptedException, IllegalArgumentException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        if (checkUserExists(user.getEmail(), user.getPhone())) {
            throw new IllegalStateException("User already exists");
        }

        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        String encodeTransactionPIN = passwordEncoder.encode(user.getTransactionPIN());
        user.setTransactionPIN(encodeTransactionPIN);

        // Ghi vào collection "users", document ID tự sinh hoặc lấy từ user.id
        ApiFuture<WriteResult> collectionsApiFuture =
                dbFirestore.collection("users").document(user.getUserID()).set(user);

        user.setPassword(null);
        user.setTransactionPIN(null);

        return user;
    }

    public boolean checkUserExists(String email, String phoneNumber) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // 1. Kiểm tra Email trước
        // .limit(1) để tối ưu hiệu năng: chỉ cần tìm thấy 1 cái là dừng ngay, không cần tìm hết
        QuerySnapshot emailQuery = db.collection("users")
                .whereEqualTo("email", email.trim())
                .limit(1)
                .get().get();

        System.out.println(emailQuery.size());
        if (!emailQuery.isEmpty()) {
            return true; // Đã tồn tại Email này
        }

        // 2. Nếu Email chưa có, kiểm tra tiếp Số điện thoại
        QuerySnapshot phoneQuery = db.collection("users")
                .whereEqualTo("phone", phoneNumber.trim()) // Đảm bảo trong Model User bạn đặt tên trường là phoneNumber
                .limit(1)
                .get().get();
        System.out.println(phoneQuery.size());

        if (!phoneQuery.isEmpty()) {
            return true; // Đã tồn tại SĐT này
        }

        // 3. Cả 2 đều chưa tồn tại
        return false;
    }

    public User loginWithPhone(String idToken) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();

        UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
        String phone =  userRecord.getPhoneNumber();

        if (phone == null) {
            throw new Exception("Phone number is null");
        }
        phone = "0" + phone.substring(3);

        Firestore dbFirestore = FirestoreClient.getFirestore();

        QuerySnapshot querySnapshot = dbFirestore.collection("users")
                                                .whereEqualTo("phone", phone)
                                                .limit(1)
                                                .get().get();

        if (!querySnapshot.isEmpty()) {
            User user = querySnapshot.getDocuments().get(0).toObject(User.class);
            user.setPassword(null);
            user.setTransactionPIN(null);
            return user;
        }
        else {
            throw new Exception("User not exist");
        }
    }

    public User login(String email, String password) throws ExecutionException, InterruptedException {
        if (password == null || password.isEmpty()) {
            return null;
        }

        Firestore dbFirestore = FirestoreClient.getFirestore();

        QuerySnapshot querySnapshot = dbFirestore.collection("users")
                                                .whereEqualTo("email", email)
                                                .limit(1)
                                                .get().get();

        if (querySnapshot == null || querySnapshot.isEmpty()) {
            return null;
        }

        User user = querySnapshot.getDocuments().get(0).toObject(User.class);
        boolean isMatch = passwordEncoder.matches(password, user.getPassword());
        if (isMatch) {
            user.setPassword(null);
            user.setTransactionPIN(null);
            return user;
        }
        else {
            return null;
        }
    }

    public Boolean checkTransactionPIN(String transactionPIN, String userId)  throws ExecutionException, InterruptedException  {
        if (transactionPIN == null || transactionPIN.isEmpty()) {
            return false;
        }

        if (userId == null || userId.isEmpty()) {
            return false;
        }

        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentSnapshot documentSnapshot = dbFirestore.collection("users").document(userId).get().get();
        if (!documentSnapshot.exists()) {
            return false;
        }

        User user = documentSnapshot.toObject(User.class);
        Boolean isMatch = passwordEncoder.matches(transactionPIN, user.getTransactionPIN());
        return isMatch;

    }

    public User updateUserProfile(String userId, UpdateUserRequest request) throws Exception {
        Firestore  dbFirestore = FirestoreClient.getFirestore();
        DocumentReference userRef = dbFirestore.collection("users").document(userId);

        // 1. Kiểm tra User có tồn tại không
        DocumentSnapshot snap = userRef.get().get();
        if (!snap.exists()) {
            throw new IllegalArgumentException("User không tồn tại");
        }

        Map<String, Object> updates = new HashMap<>();

        // Chỉ put các trường nếu dữ liệu gửi lên khác null và không rỗng
        if (request.getFullName() != null) updates.put("fullName", request.getFullName());
        if (request.getDateOfBirth() != null) updates.put("dateOfBirth", request.getDateOfBirth());
        if (request.getNationalID() != null) updates.put("nationalID", request.getNationalID());
        if (request.getEmail() != null) updates.put("email", request.getEmail());
        if (request.getPhone() != null) updates.put("phone", request.getPhone());
        if (request.getAddress() != null) updates.put("address", request.getAddress());
        if (request.getOccupation() != null) updates.put("occupation", request.getOccupation());
        if (request.getCompanyName() != null) updates.put("companyName", request.getCompanyName());
        if (request.getAverageSalary() != null) updates.put("averageSalary", request.getAverageSalary());

        updates.put("updatedAt", new Date());

        WriteResult result = userRef.update(updates).get();

        // 4. Lấy lại dữ liệu mới nhất để trả về cho Client hiển thị
        DocumentSnapshot updatedSnap = userRef.get().get();
        return updatedSnap.toObject(User.class);
    }

    public void changeTransactionPin(ChangePINRequest request) throws Exception {
        Firestore  dbFirestore = FirestoreClient.getFirestore();

        if (request.getNewPin() == null || request.getNewPin().length() != 6) {
            throw new IllegalArgumentException("Mã PIN mới phải bao gồm 6 chữ số");
        }

        if (!request.getNewPin().equals(request.getConfirmNewPin())) {
            throw new IllegalArgumentException("Mã PIN xác nhận không khớp");
        }

        DocumentReference userRef = dbFirestore.collection("users").document(request.getUserId());
        DocumentSnapshot snap = userRef.get().get();

        if (!snap.exists()) {
            throw new IllegalArgumentException("User không tồn tại");
        }

        String currentPinInDb = snap.getString("transactionPIN");

        if (currentPinInDb != null && !passwordEncoder.matches(request.getOldPin(),  currentPinInDb)) {
            throw new IllegalArgumentException("Mã PIN cũ không chính xác");
        }

        if (currentPinInDb != null && currentPinInDb.equals(request.getNewPin())) {
            throw new IllegalArgumentException("Mã PIN mới không được trùng với mã PIN hiện tại");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("transactionPIN", passwordEncoder.encode(request.getNewPin()));
        updates.put("updatedAt", new Date());

        userRef.update(updates).get();
    }
}
