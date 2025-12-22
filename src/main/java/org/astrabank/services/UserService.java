package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.iid.FirebaseInstanceId;
import org.astrabank.constant.AccountType;
import org.astrabank.dto.*;
import org.astrabank.models.Account;
import org.astrabank.models.Staff;
import org.astrabank.models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private  final AccountService accountService;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.accountService = new AccountService(passwordEncoder);
    }

    public Staff createStaff(StaffRequest staffRequest)  throws ExecutionException, InterruptedException, IllegalArgumentException  {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        if (checkUserExists(staffRequest.getEmail(), staffRequest.getPhone(), staffRequest.getNationalID())) {
            throw new IllegalStateException("User already exists");
        }

        Staff staff = new Staff();
        staff.setUserID(staffRequest.getUserID());
        staff.setFullName(staffRequest.getFullName());
        staff.setDateOfBirth(staffRequest.getDateOfBirth());
        staff.setNationalID(staffRequest.getNationalID());
        staff.setEmail(staffRequest.getEmail());
        staff.setPhone(staffRequest.getPhone());
        staff.setAddress(staffRequest.getAddress());

        String pin = passwordEncoder.encode(staffRequest.getTransactionPIN());
        System.out.println("pin: " + staff.getTransactionPIN());
        staff.setTransactionPIN(pin);
        System.out.println(pin);

        staff.setStatus(true);
        staff.setRole("OFFICER");
        staff.setUpdateBy(staffRequest.getUpdateBy());
        staff.setCreatedBy(staffRequest.getUpdateBy());
        staff.setCreatedAt(new Date());
        staff.setUpdatedAt(new Date());
        ApiFuture<WriteResult> collectionsApiFuture =
                dbFirestore.collection("users").document(staff.getUserID()).set(staff);

        staff.setTransactionPIN(null);

        return staff;
    }

    public User createCustomer(CustomerRequest customerRequest) throws ExecutionException, InterruptedException, IllegalArgumentException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        System.out.println(customerRequest.toString());

        if (checkUserExists(customerRequest.getEmail(), customerRequest.getPhone(), customerRequest.getNationalID())) {
            throw new IllegalStateException("User already exists");
        }

        User user = new User();
        user.setUserID(customerRequest.getUserID());
        user.setFullName(customerRequest.getFullName());
        user.setDateOfBirth(customerRequest.getDateOfBirth());
        user.setNationalID(customerRequest.getNationalID());
        user.setEmail(customerRequest.getEmail());
        user.setPhone(customerRequest.getPhone());
        user.setAddress(customerRequest.getAddress());
        user.setOccupation(customerRequest.getOccupation());
        user.setCompanyName(customerRequest.getCompanyName());
        user.setAverageSalary(customerRequest.getAverageSalary());
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        user.setRole("CUSTOMER");
        String encodeTransactionPIN = passwordEncoder.encode(customerRequest.getTransactionPIN());
        user.setTransactionPIN(encodeTransactionPIN);
        user.setStatus(true);
        user.setCreatedBy(customerRequest.getCreatedBy());
        user.setUpdateBy(customerRequest.getUpdatedBy());

        // Ghi vào collection "users", document ID tự sinh hoặc lấy từ user.id
        ApiFuture<WriteResult> collectionsApiFuture =
                dbFirestore.collection("users").document(user.getUserID()).set(user);

        user.setTransactionPIN(null);

        accountService.createAccount(new AccountRequest(user.getUserID(), AccountType.CHECKING));

        return user;
    }

    public boolean checkUserExists(String email, String phoneNumber, String nationalID) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // 1. Kiểm tra Email trước
        // .limit(1) để tối ưu hiệu năng: chỉ cần tìm thấy 1 cái là dừng ngay, không cần tìm hết
        QuerySnapshot emailQuery = db.collection("users")
                .whereEqualTo("email", email.trim())
                .limit(1)
                .get().get();

        System.out.println(emailQuery.size());
        if (!emailQuery.isEmpty()) {
            return true;
        }

        // 2. Nếu Email chưa có, kiểm tra tiếp Số điện thoại
        QuerySnapshot phoneQuery = db.collection("users")
                .whereEqualTo("phone", phoneNumber.trim())
                .limit(1)
                .get().get();
        System.out.println(phoneQuery.size());

        if (!phoneQuery.isEmpty()) {
            return true;
        }

        QuerySnapshot nationalIDQuery = db.collection("users")
                .whereEqualTo("nationalID", nationalID.trim()) // Đảm bảo trong Model User bạn đặt tên trường là phoneNumber
                .limit(1)
                .get().get();
        System.out.println(nationalIDQuery.size());

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
            user.setTransactionPIN(null);
            return user;
        }
        else {
            throw new Exception("User not exist");
        }
    }

    public User login(String email, String pin) throws ExecutionException, InterruptedException {
        if (pin == null || pin.isEmpty()) {
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
        boolean isMatch = passwordEncoder.matches(pin, user.getTransactionPIN());
        if (isMatch) {
            user.setTransactionPIN(null);
            return user;
        }
        else {
            return null;
        }
    }

    public User findAccountByEmail(String email) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        Query query = dbFirestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1); // Chỉ lấy 1 kết quả đầu tiên

        QuerySnapshot querySnapshot = query.get().get();

        if (!querySnapshot.isEmpty()) {
            return querySnapshot.getDocuments().get(0).toObject(User.class);
        } else {
            return null;
        }
    }

    public User findUserById(String userId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        DocumentReference docRef = dbFirestore.collection("users").document(userId);

        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            User user = document.toObject(User.class);
            user.setTransactionPIN(null);
            return user;
        } else {
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
        if (request.getUpdatedBy() != null) updates.put("updatedBy", request.getUpdatedBy());
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

    public List<User> getAllCustomers() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        QuerySnapshot querySnapshot = dbFirestore.collection("users")
                .whereEqualTo("role", "CUSTOMER")
                .get()
                .get();

        return querySnapshot.toObjects(User.class);
    }

    public User createUserForAdmin(AdminCustomerRequest customerRequestForAdmin) throws Exception {
        Firestore  dbFirestore = FirestoreClient.getFirestore();

        return dbFirestore.runTransaction(new Transaction.Function<User>() {
            @Override
            public User updateCallback(Transaction transaction) throws Exception {
                CustomerRequest customerRequest = new CustomerRequest();
                customerRequest.setUserID(customerRequestForAdmin.getUserID());
                customerRequest.setFullName(customerRequestForAdmin.getFullName());
                customerRequest.setDateOfBirth(customerRequestForAdmin.getDateOfBirth());
                customerRequest.setNationalID(customerRequestForAdmin.getNationalID());
                customerRequest.setEmail(customerRequestForAdmin.getEmail());
                customerRequest.setPhone(customerRequestForAdmin.getPhone());
                customerRequest.setAddress(customerRequestForAdmin.getAddress());
                customerRequest.setOccupation(customerRequestForAdmin.getOccupation());
                customerRequest.setCompanyName(customerRequestForAdmin.getCompanyName());
                customerRequest.setAverageSalary(customerRequestForAdmin.getAverageSalary());
                customerRequest.setTransactionPIN(customerRequestForAdmin.getTransactionPIN());
                customerRequest.setCreatedBy(customerRequestForAdmin.getCreatedBy());
                customerRequest.setUpdatedBy(customerRequestForAdmin.getUpdatedBy());
                User user = createCustomer(customerRequest);

                accountService.createAccount(new AccountRequest(customerRequestForAdmin.getUserID(), AccountType.CHECKING), customerRequestForAdmin.getDeposit());

                return user;
            }
        }).get();
    }

    public boolean deactivateUserSystem(String userId, boolean status) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            DocumentReference userRef = db.collection("users").document(userId);
            DocumentSnapshot userSnap = userRef.get().get();

            if (!userSnap.exists()) {
                throw new RuntimeException("User not found");
            }

            WriteBatch batch = db.batch();

            batch.update(userRef, "status", status);

            QuerySnapshot accountQuery = db.collection("accounts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            List<QueryDocumentSnapshot> accounts = accountQuery.getDocuments();

            for (DocumentSnapshot acc : accounts) {
                batch.update(acc.getReference(), "accountStatus", status);
            }
            batch.commit().get();

            return true;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lỗi hệ thống khi cập nhật dữ liệu: " + e.getMessage());
        }
    }

    public List<User> getCustomersWithoutMortgage() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Query queryCustomers = db.collection("users")
                .whereEqualTo("role", "CUSTOMER");

        List<QueryDocumentSnapshot> customerDocs = queryCustomers.get().get().getDocuments();

        Query queryMortgages = db.collection("accounts")
                .whereEqualTo("accountType", "MORTGAGE");

        List<QueryDocumentSnapshot> mortgageDocs = queryMortgages.get().get().getDocuments();

        Set<String> userIdsWithMortgage = new HashSet<>();
        for (DocumentSnapshot doc : mortgageDocs) {
            String ownerId = doc.getString("userId");
            if (ownerId != null) {
                userIdsWithMortgage.add(ownerId);
            }
        }

        List<User> finalResult = new ArrayList<>();

        for (DocumentSnapshot doc : customerDocs) {
            String currentUserId = doc.getString("userID");

            if (currentUserId != null && !userIdsWithMortgage.contains(currentUserId)) {
                User user = doc.toObject(User.class);
                finalResult.add(user);
            }
        }

        return finalResult;
    }

}


