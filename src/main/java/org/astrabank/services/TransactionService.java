package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.constant.TransactionStatus;
import org.astrabank.constant.TransactionType;
import org.astrabank.dto.AccountResponse;
import org.astrabank.dto.TransactionRequest;
import org.astrabank.models.Bank;
import org.astrabank.models.Notification;
import org.astrabank.models.Transaction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class TransactionService {

    private final AccountService accountService;
    private final String BANK_DESTINATION_SYMBOL = "ATB";
    private final BankService bankService;
    private final RestTemplate restTemplate = new RestTemplate();


    public TransactionService(AccountService accountService, BankService bankService) {
        this.accountService = accountService;
        this.bankService = bankService;
    }

    public Transaction progressTransferring(TransactionRequest transactionRequest)
            throws ExecutionException, InterruptedException, NullPointerException, IllegalArgumentException
    {
//        if (!transactionRequest.getDestinationBankSymbol().equals(BANK_DESTINATION_SYMBOL)) {
//            System.out.println("Wrong destination bank symbol");
//            throw new IllegalArgumentException("Wrong destination bank symbol");
//        }

        // check balance
        Boolean checkBalance = accountService.checkBalance(
                transactionRequest.getSourceAccountNumber(),
                transactionRequest.getAmount());

        if (!checkBalance) {
            throw new IllegalArgumentException("Balance not available");
        }

        // tao giao dich
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Transaction transaction = new Transaction();
        DocumentReference documentReference =  dbFirestore.collection("transactions").document();
        transaction.setTransactionId(documentReference.getId());
        transaction.setSourceAcc(transactionRequest.getSourceAccountNumber());
        transaction.setBankSourceSymbol(transactionRequest.getSourceBankSymbol());
        transaction.setDestinationAcc(transactionRequest.getDestinationAccountNumber());
        transaction.setBankDesSymbol(transactionRequest.getDestinationBankSymbol());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setType(transactionRequest.getTransactionType());
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());
        transaction.setSenderName(transactionRequest.getSenderName());
        transaction.setReceiverName(transactionRequest.getReceiverName());

        ApiFuture<WriteResult> future = dbFirestore
                .collection("transactions")
                .document(transaction.getTransactionId())
                .set(transaction);
        WriteResult result = future.get();

        // tru tien
        // cong tien

//        try {
//            dbFirestore.runTransaction(new com.google.cloud.firestore.Transaction.Function<Object>() {
//                @Override
//                public Object updateCallback(com.google.cloud.firestore.Transaction transaction) throws Exception {
//                    return null;
//                }
//            });
//
//            accountService.subtractBalance(transaction.getSourceAcc(), transaction.getAmount()).get();
//            wasSubtracted = true;
//
//            accountService.addBalance(transaction.getDestinationAcc(), transaction.getAmount()).get();
//            wasAdded = true;
//
//            transaction.setStatus(TransactionStatus.SUCCESS);
//            transaction.setUpdatedAt(new Date());
//
//            // Lưu trạng thái mới xuống DB
//            dbFirestore.collection("transactions").document(transaction.getTransactionId()).set(transaction);
//
//            // (Gửi mail ở đây nếu cần...)
//
//            return transaction;
//        }
//        catch (InterruptedException | ExecutionException e) {
//            dbFirestore.collection("transactions").document(transaction.getTransactionId()).update("status", "FAILED");
//
//            // refund...
//            if (wasSubtracted && !wasAdded) {
//                // refund
//            }
//
//            throw new RuntimeException("Giao dịch thất bại: " + e.getMessage());
//        }
        try {
            dbFirestore.runTransaction(new com.google.cloud.firestore.Transaction.Function<Object>() {
                @Override
                public Object updateCallback(com.google.cloud.firestore.Transaction t) throws Exception {

                    DocumentReference sourceRef = dbFirestore.collection("accounts").document(transactionRequest.getSourceAccountNumber());
                    DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());

                    DocumentSnapshot sourceSnap = t.get(sourceRef).get();

                    Long currentBalance = sourceSnap.getLong("balance");
                    if (currentBalance == null || currentBalance < transactionRequest.getAmount()) {
                        throw new IllegalArgumentException("Số dư không đủ (Check in Transaction)");
                    }

                    accountService.deductBalanceTx(t, transactionRequest.getSourceAccountNumber(), transactionRequest.getAmount());
                    accountService.addBalanceTx(t, transactionRequest.getDestinationAccountNumber(), transactionRequest.getAmount());

                    t.update(transRef, "status", "SUCCESS");
                    t.update(transRef, "updatedAt", new Date());

                    return null;
                }
            }).get();

            transaction.setStatus(TransactionStatus.SUCCESS);
            return transaction;

        } catch (Exception e) {
            dbFirestore.collection("transactions").document(transaction.getTransactionId()).update("status", "FAILED");

            throw new RuntimeException("Giao dịch thất bại: " + e.getMessage());
        }
    }

    public Transaction receiveTransfer(TransactionRequest transactionRequest)
            throws ExecutionException, InterruptedException, NullPointerException, IllegalArgumentException
    {
        // kiem tra source bank
        boolean isExist = bankService.checkBank(transactionRequest.getSourceBankSymbol());
        if (!isExist) {
            throw new IllegalArgumentException("Wrong source bank symbol");
        }

        // check destination bank
        if (!transactionRequest.getDestinationBankSymbol().equals(BANK_DESTINATION_SYMBOL)) {
            System.out.println("Wrong destination bank symbol");
            throw new IllegalArgumentException("Wrong destination bank symbol");
        }

        // kiem tra so tai khoan
        AccountResponse accountResponse = accountService.findAccount(transactionRequest.getDestinationAccountNumber());
        if (accountResponse == null) {
            throw new IllegalArgumentException("Account not found");
        }

        // tao giao dich
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Transaction transaction = new Transaction();
        DocumentReference documentReference =  dbFirestore.collection("transactions").document();
        transaction.setTransactionId(documentReference.getId());
        transaction.setSourceAcc(transactionRequest.getSourceAccountNumber());
        transaction.setBankSourceSymbol(transactionRequest.getSourceBankSymbol());
        transaction.setDestinationAcc(transactionRequest.getDestinationAccountNumber());
        transaction.setBankDesSymbol(transactionRequest.getDestinationBankSymbol());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setType(transactionRequest.getTransactionType());
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());
        transaction.setSenderName(transactionRequest.getSenderName());
        transaction.setReceiverName(transactionRequest.getReceiverName());

        ApiFuture<WriteResult> future = dbFirestore
                .collection("transactions")
                .document(transaction.getTransactionId())
                .set(transaction);
        WriteResult result = future.get();

        // nhan tien
        // update trang thai
        try {
            dbFirestore.runTransaction(new com.google.cloud.firestore.Transaction.Function<Object>() {
                @Override
                public Object updateCallback(com.google.cloud.firestore.Transaction t) throws Exception {

                    DocumentReference sourceRef = dbFirestore.collection("accounts").document(transactionRequest.getSourceAccountNumber());
                    DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());

                    DocumentSnapshot sourceSnap = t.get(sourceRef).get();

                    accountService.addBalanceTx(t, transactionRequest.getDestinationAccountNumber(), transactionRequest.getAmount());

                    t.update(transRef, "status", "SUCCESS");
                    t.update(transRef, "updatedAt", new Date());

                    return null;
                }
            }).get();

            transaction.setStatus(TransactionStatus.SUCCESS);
            return transaction;

        } catch (Exception e) {
            dbFirestore.collection("transactions").document(transaction.getTransactionId()).update("status", "FAILED");

            throw new RuntimeException("Giao dịch thất bại: " + e.getMessage());
        }

    }

    public Transaction sendTransaction(TransactionRequest transactionRequest)
            throws Exception
    {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        // check balance
        Boolean checkBalance = accountService.checkBalance(
                transactionRequest.getSourceAccountNumber(),
                transactionRequest.getAmount());

        if (!checkBalance) {
            throw new IllegalArgumentException("Balance not available");
        }

        // check bank
        Bank bank;
        DocumentSnapshot bankDocRef  = dbFirestore.collection("banks").document(transactionRequest.getDestinationBankSymbol()).get().get();
        if (bankDocRef.exists()) {
            bank = bankDocRef.toObject(Bank.class);
        }
        else {
            throw new IllegalArgumentException("Bank not found");
        }

        if (bank == null) {
            throw new IllegalArgumentException("Bank not found");
        }

        // tao giao dich
        Transaction transaction = new Transaction();
        DocumentReference documentReference =  dbFirestore.collection("transactions").document();
        transaction.setTransactionId(documentReference.getId());
        transaction.setSourceAcc(transactionRequest.getSourceAccountNumber());
        transaction.setBankSourceSymbol(transactionRequest.getSourceBankSymbol());
        transaction.setDestinationAcc(transactionRequest.getDestinationAccountNumber());
        transaction.setBankDesSymbol(transactionRequest.getDestinationBankSymbol());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setType(transactionRequest.getTransactionType());
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());
        transaction.setSenderName(transactionRequest.getSenderName());
        transaction.setReceiverName(transactionRequest.getReceiverName());

        ApiFuture<WriteResult> future = dbFirestore
                .collection("transactions")
                .document(transaction.getTransactionId())
                .set(transaction);
        WriteResult result = future.get();
        // nhan tien
        // update trang thai
//        try {
//            dbFirestore.runTransaction(new com.google.cloud.firestore.Transaction.Function<Object>() {
//                @Override
//                public Object updateCallback(com.google.cloud.firestore.Transaction t) throws Exception {
//                    DocumentReference sourceRef = dbFirestore.collection("accounts").document(transactionRequest.getSourceAccountNumber());
//                    DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());
//
//                    DocumentSnapshot sourceSnap = t.get(sourceRef).get();
//
//                    accountService.deductBalanceTx(t, transactionRequest.getDestinationAccountNumber(), transactionRequest.getAmount());
//
//                    t.update(transRef, "status", "PENDING");
//
//                    return null;
//                }
//            }).get();
//        } catch (Exception e) {
//            throw new Exception("Giao dịch thất bại: " + e.getMessage());
//        }
//
//        String link = bank.getLinkApiTransfer();
//        Object sendRequest = callUrl(link, transactionRequest);
//
//        if (sendRequest == null) {
//            throw new Exception("Transaction failed");
//        }
//
//        try {
//            dbFirestore.runTransaction(t -> {
//                DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());
//                DocumentReference sourceRef = dbFirestore.collection("accounts").document(transaction.getSourceAcc());
//
//                if (sendRequest != null) {
//                    t.update(transRef, "status", "SUCCESS");
//                    t.update(transRef, "updatedAt", new Date());
//                } else {
//                    // B. Nếu API thất bại -> HOÀN TIỀN (Bù lại số đã trừ ở Bước 1)
//                    accountService.addBalanceTx(t, transaction.getSourceAcc(), transactionRequest.getAmount());
//
//                    t.update(transRef, "status", "FAILED");
//                    t.update(transRef, "note", "Hoàn tiền do lỗi phía đối tác");
//                }
//                return null;
//            }).get();
//
//        } catch (Exception e) {
//            // Lỗi cực nghiêm trọng: Đã chuyển tiền/trừ tiền nhưng không update được DB
//            // Cần log ra file riêng để nhân viên IT vào sửa thủ công (Reconciliation)
//            System.err.println("CRITICAL ERROR: Data inconsistent for TX " + transaction.getTransactionId());
//        }
//
//        return null;

        try {
            dbFirestore.runTransaction(t -> {
                // 1. Lấy Reference
                DocumentReference sourceRef = dbFirestore.collection("accounts").document(transactionRequest.getSourceAccountNumber());
                DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());

                // 2. Gọi service trừ tiền (LƯU Ý: Phải trừ tiền của SOURCE, không phải Destination)
                // Mình sửa lại tham số thành SourceAccountNumber
                accountService.deductBalanceTx(t, transactionRequest.getSourceAccountNumber(), transactionRequest.getAmount());

                // 3. Update trạng thái PENDING
                t.update(transRef, "status", "PENDING");
                t.update(transRef, "updatedAt", new Date());

                return null;
            }).get();

        } catch (Exception e) {
            // Nếu lỗi ngay từ bước trừ tiền -> Dừng luôn, báo lỗi về Client
            throw new Exception("Khởi tạo giao dịch thất bại: " + e.getMessage());
        }

        boolean isPartnerSuccess = false;
        String note = "";

        try {
            String link = bank.getLinkApiTransfer();
            // Gọi hàm callUrl đã viết trước đó
            Object response = callUrl(link, transactionRequest);

            if (response != null) {
                isPartnerSuccess = true;
            } else {
                isPartnerSuccess = false;
                note = "Đối tác không phản hồi hoặc trả về null";
            }
        } catch (Exception e) {
            isPartnerSuccess = false;
            note = "Lỗi gọi API đối tác: " + e.getMessage();
            e.printStackTrace();
        }

        // Biến final để dùng trong lambda
        final boolean successFlag = isPartnerSuccess;
        final String failNote = note;

        try {
            dbFirestore.runTransaction(t -> {
                DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());
                // Lấy lại ref người gửi để hoàn tiền nếu cần
                DocumentReference sourceRef = dbFirestore.collection("accounts").document(transactionRequest.getSourceAccountNumber());

                if (successFlag) {
                    // A. NẾU THÀNH CÔNG -> Update Status SUCCESS
                    t.update(transRef, "status", "SUCCESS");
                    t.update(transRef, "updatedAt", new Date());
                } else {
                    // B. NẾU THẤT BẠI -> HOÀN TIỀN (Compensating Transaction)

                    // Cộng lại tiền vào tài khoản nguồn
                    accountService.addBalanceTx(t, transactionRequest.getSourceAccountNumber(), transactionRequest.getAmount());

                    // Update Status FAILED
                    t.update(transRef, "status", "FAILED");
                    t.update(transRef, "note", failNote); // Ghi chú lý do lỗi
                    t.update(transRef, "updatedAt", new Date());
                }
                return null;
            }).get();

        } catch (Exception e) {
            // Lỗi CỰC KỲ NGHIÊM TRỌNG (Critical Error)
            // Tiền đã trừ (Bước 1) -> Gọi API xong (Bước 2) -> Nhưng không update được DB (Bước 3)
            // Cần log riêng ra file hoặc bắn thông báo cho Admin ngay lập tức
            System.err.println("CRITICAL ERROR [DATA INCONSISTENT]: TransactionID " + transaction.getTransactionId());
            e.printStackTrace();
        }

        // Cập nhật lại trạng thái vào object transaction để trả về controller
        transaction.setStatus(successFlag ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
        return transaction;
    }

    public Object callUrl(String url, Object requestBody) throws Exception {
        // 1. Kiểm tra URL
        if (url == null || url.isEmpty()) {
            throw new Exception("URL không hợp lệ (null hoặc rỗng)");
        }

        try {
            // 2. Cấu hình Header là JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. Đóng gói dữ liệu gửi đi
            // Nếu requestBody là null, nó vẫn gửi POST với body rỗng (hợp lệ)
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

            // 4. Thực hiện gọi POST
            // restTemplate.postForObject sẽ trả về kết quả map vào Object (Map hoặc List)
            return restTemplate.postForObject(url, entity, Object.class);

        } catch (Exception e) {
            // Log lỗi ra để dễ debug
            e.printStackTrace();
            throw new Exception("Lỗi khi gọi POST tới " + url + ": " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionHistory(String accountNumber) throws Exception {
        List<Transaction> allTransactions = new ArrayList<>();

        try {
            Firestore dbFirestore = FirestoreClient.getFirestore();

            Query querySent = dbFirestore.collection("transactions").whereEqualTo("sourceAcc", accountNumber);
            //.whereEqualTo("status", "SUCCESS");

            // 2. Tìm các giao dịch là NGƯỜI NHẬN (Tiền về)
            Query queryReceived = dbFirestore.collection("transactions").whereEqualTo("destinationAcc", accountNumber);
            //.whereEqualTo("status", "SUCCESS");

            // 3. Thực thi 2 câu lệnh song song (Asynchronous) để tiết kiệm thời gian
            ApiFuture<QuerySnapshot> futureSent = querySent.get();
            ApiFuture<QuerySnapshot> futureReceived = queryReceived.get();

            // 4. Lấy dữ liệu và map sang Object
            List<QueryDocumentSnapshot> sentDocs = futureSent.get().getDocuments();
            List<QueryDocumentSnapshot> receivedDocs = futureReceived.get().getDocuments();

            for (DocumentSnapshot doc : sentDocs) {
                Transaction t = doc.toObject(Transaction.class);
                allTransactions.add(t);
            }

            for (DocumentSnapshot doc : receivedDocs) {
                Transaction t = doc.toObject(Transaction.class);
                // t.setTransactionId(doc.getId());
                allTransactions.add(t);
            }

            allTransactions.sort((t1, t2) -> {
                if (t1.getCreatedAt() == null || t2.getCreatedAt() == null) return 0;
                return t2.getCreatedAt().compareTo(t1.getCreatedAt()); // Giảm dần (Descending)
            });

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy lịch sử giao dịch: " + e.getMessage());
        }

        return allTransactions;
    }

    public Transaction processDeposit(TransactionRequest transactionRequest)
            throws ExecutionException, InterruptedException, IllegalArgumentException {

        // 1. Kiểm tra số tiền nạp phải hợp lệ (> 0)
        if (transactionRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }

        // 2. Tạo bản ghi Transaction (Trạng thái PENDING)
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Transaction transaction = new Transaction();
        DocumentReference documentReference = dbFirestore.collection("transactions").document();

        transaction.setTransactionId(documentReference.getId());

        // Với nạp tiền: Source là hệ thống/cổng thanh toán, Destination là tài khoản User
        transaction.setSourceAcc("SYSTEM_DEPOSIT"); // Hoặc lấy từ cổng thanh toán (Momo, Stripe...)
        transaction.setBankSourceSymbol("INTERNAL");

        transaction.setDestinationAcc(transactionRequest.getDestinationAccountNumber()); // Tài khoản nhận tiền
        transaction.setBankDesSymbol(transactionRequest.getDestinationBankSymbol());

        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setType(TransactionType.DEPOSIT); // Hoặc dùng transactionRequest.getTransactionType() nếu đã set
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        // Các trường tên người gửi/nhận
        transaction.setSenderName("SYSTEM");
        transaction.setReceiverName(transactionRequest.getReceiverName());

        // Lưu transaction xuống DB với trạng thái PENDING
        ApiFuture<WriteResult> future = dbFirestore
                .collection("transactions")
                .document(transaction.getTransactionId())
                .set(transaction);
        future.get(); // Chờ lưu xong

        // 3. Thực hiện Transaction trong Firestore (Cộng tiền)
        try {
            dbFirestore.runTransaction(t -> {
                DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());

                // --- KHÔNG CẦN CHECK BALANCE VÌ ĐÂY LÀ NẠP TIỀN ---

                // Gọi service cộng tiền (Reuse lại hàm có sẵn của bạn)
                accountService.addBalanceTx(t, transactionRequest.getDestinationAccountNumber(), transactionRequest.getAmount());

                // Cập nhật trạng thái giao dịch thành công
                t.update(transRef, "status", "SUCCESS");
                t.update(transRef, "updatedAt", new Date());

                return null;
            }).get();

            // Cập nhật object trả về
            transaction.setStatus(TransactionStatus.SUCCESS);
            return transaction;

        } catch (Exception e) {
            // Nếu lỗi, cập nhật trạng thái FAILED
            dbFirestore.collection("transactions").document(transaction.getTransactionId()).update("status", "FAILED");

            throw new RuntimeException("Nạp tiền thất bại: " + e.getMessage());
        }
    }

    public Transaction processWithdrawal(TransactionRequest transactionRequest)
            throws ExecutionException, InterruptedException, IllegalArgumentException {

        // 1. Kiểm tra đầu vào
        if (transactionRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        }

        // 2. Kiểm tra số dư sơ bộ (Optional - Để báo lỗi nhanh cho User đỡ phải gọi xuống DB sâu)
        // Lưu ý: Việc kiểm tra chính xác 100% sẽ nằm trong Transaction bên dưới
        Boolean isBalanceEnough = accountService.checkBalance(
                transactionRequest.getSourceAccountNumber(),
                transactionRequest.getAmount()
        );
        if (!isBalanceEnough) {
            throw new IllegalArgumentException("Số dư không đủ để thực hiện giao dịch");
        }

        // 3. Tạo Transaction Record (Trạng thái PENDING)
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Transaction transaction = new Transaction();
        DocumentReference documentReference = dbFirestore.collection("transactions").document();

        transaction.setTransactionId(documentReference.getId());

        // RÚT TIỀN: Source là User, Destination là ATM/System
        transaction.setSourceAcc(transactionRequest.getSourceAccountNumber());
        transaction.setBankSourceSymbol(transactionRequest.getSourceBankSymbol()); // Bank của User

        transaction.setDestinationAcc("ATM_WITHDRAWAL"); // Hoặc ID cây ATM
        transaction.setBankDesSymbol("CASH");

        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        transaction.setSenderName(transactionRequest.getSenderName());
        transaction.setReceiverName("Cash Withdrawal");

        // Lưu PENDING xuống DB
        ApiFuture<WriteResult> future = dbFirestore
                .collection("transactions")
                .document(transaction.getTransactionId())
                .set(transaction);
        future.get();

        // 4. Thực hiện Transaction Firestore (Trừ tiền an toàn)
        try {
            dbFirestore.runTransaction(t -> {
                DocumentReference transRef = dbFirestore.collection("transactions").document(transaction.getTransactionId());
                DocumentReference sourceRef = dbFirestore.collection("accounts").document(transactionRequest.getSourceAccountNumber());

                // A. Đọc số dư mới nhất trong Transaction (Quan trọng để tránh Race Condition)
                DocumentSnapshot sourceSnap = t.get(sourceRef).get();
                Double currentBalance = sourceSnap.getDouble("balance"); // Hoặc getLong tuỳ DB bạn lưu

                if (currentBalance == null || currentBalance < transactionRequest.getAmount()) {
                    throw new IllegalArgumentException("Số dư không đủ (Kiểm tra lại lúc giao dịch)");
                }

                // B. Trừ tiền tài khoản User
                accountService.deductBalanceTx(t, transactionRequest.getSourceAccountNumber(), transactionRequest.getAmount());

                // C. Update trạng thái thành công
                t.update(transRef, "status", "SUCCESS");
                t.update(transRef, "updatedAt", new Date());

                return null;
            }).get();

            transaction.setStatus(TransactionStatus.SUCCESS);
            return transaction;

        } catch (Exception e) {
            // Nếu lỗi (không đủ tiền, lỗi DB...), cập nhật FAILED
            dbFirestore.collection("transactions").document(transaction.getTransactionId()).update("status", "FAILED");

            // Ném lỗi ra để Controller bắt
            throw new RuntimeException("Rút tiền thất bại: " + e.getMessage());
        }
    }

    public List<Notification> getAllTransactionsByUserId(String userId) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        List<Transaction> finalResult = new ArrayList<>();
        Set<String> processedTransactionIds = new HashSet<>(); // Dùng Set để lọc trùng lặp
        List<Notification> notifications = new ArrayList<>();


        try {
            List<QueryDocumentSnapshot> accountDocs = dbFirestore.collection("accounts")
                    .whereEqualTo("userId", userId)
                    .get().get().getDocuments();

            if (accountDocs.isEmpty()) {
                return new ArrayList<>(); // User chưa có tài khoản nào
            }

            List<String> userAccountNumbers = new ArrayList<>();
            for (DocumentSnapshot doc : accountDocs) {
                userAccountNumbers.add(doc.getId());
            }

            List<ApiFuture<QuerySnapshot>> futures = new ArrayList<>();
            CollectionReference transRef = dbFirestore.collection("transactions");

            for (String accNum : userAccountNumbers) {
                futures.add(transRef.whereEqualTo("sourceAcc", accNum).get());

                futures.add(transRef.whereEqualTo("destinationAcc", accNum).get());
            }

            for (ApiFuture<QuerySnapshot> future : futures) {
                List<QueryDocumentSnapshot> docs = future.get().getDocuments();

                for (DocumentSnapshot doc : docs) {
                    Transaction t = doc.toObject(Transaction.class);

                    if (!processedTransactionIds.contains(t.getTransactionId())) {
                        finalResult.add(t);
                        processedTransactionIds.add(t.getTransactionId());
                    }
                }
            }

            finalResult.sort((t1, t2) -> {
                if (t1.getCreatedAt() == null || t2.getCreatedAt() == null) return 0;
                return t2.getCreatedAt().compareTo(t1.getCreatedAt());
            });

            for (Transaction t : finalResult) {

                if (userAccountNumbers.contains(t.getSourceAcc()) && userAccountNumbers.contains(t.getDestinationAcc())) {
                    String content = t.getSourceAcc() + " CHUYEN TIEN DEN TAI KHOAN " + t.getDestinationAcc() + " SO TIEN " + t.getAmount() + " LUC " + t.getCreatedAt() + ". MA GIAO DICH: " + t.getTransactionId();
                    String title = "BIEN DONG SO DU";
                    String amount = String.valueOf(t.getAmount());
                    notifications.add(new Notification(content, title,  amount));
                }
                else if (userAccountNumbers.contains(t.getSourceAcc()) && !userAccountNumbers.contains(t.getDestinationAcc())){
                    String title = "BIEN DONG SO DU -" + t.getAmount();
                    String content = t.getSourceAcc() + " CHUYEN TIEN DEN TAI KHOAN CUA " + t.getReceiverName() + " SO TIEN " + t.getAmount() + " LUC " + t.getCreatedAt() + ". NOI DUNG: " + t.getDescription() + ". MA GIAO DICH: " + t.getTransactionId();
                    String amount = "-" +  t.getAmount();
                    notifications.add(new Notification(content, title, amount));
                }
                else  if (!userAccountNumbers.contains(t.getSourceAcc()) && userAccountNumbers.contains(t.getDestinationAcc())) {
                    String title = "BIEN DONG SO DU +" + t.getAmount();
                    String content = t.getSourceAcc() + " CHUYEN DEN TAI KHOAN CUA BAN " + t.getDestinationAcc() + " SO TIEN " + t.getAmount() + " LUC " + t.getCreatedAt() + ". NOI DUNG: " + t.getDescription() + ". MA GIAO DICH: " + t.getTransactionId();
                    String amount = "+" +  t.getAmount();
                    notifications.add(new Notification(content, title,  amount));
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi lấy danh sách giao dịch: " + e.getMessage());
        }

        return notifications;
    }
}