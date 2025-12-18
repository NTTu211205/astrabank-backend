package org.astrabank.services;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.constant.TransactionStatus;
import org.astrabank.constant.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.astrabank.models.Transaction;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class InterestScheduler {

    private final Firestore dbFirestore = FirestoreClient.getFirestore();

    // Các hằng số
    private static final String COLLECTION_ACCOUNTS = "accounts";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String COLLECTION_USERS = "users"; // Collection chứa thông tin user

    @Scheduled(cron = "0 0 0 1 * ?")
//     @Scheduled(fixedRate = 60000)
    public void payMonthlyDemandInterest() {
        System.out.println("--- [SCHEDULER] BẮT ĐẦU TRẢ LÃI KHÔNG KỲ HẠN ---");

        try {
            List<QueryDocumentSnapshot> accounts = dbFirestore.collection(COLLECTION_ACCOUNTS)
                    .whereEqualTo("accountType", "SAVING")
                    .whereEqualTo("accountStatus", true)
                    .get().get().getDocuments();

            if (accounts.isEmpty()) {
                System.out.println("[SCHEDULER] Không có tài khoản nào.");
                return;
            }

            for (DocumentSnapshot doc : accounts) {
                processInterestForAccount(doc);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void processInterestForAccount(DocumentSnapshot doc) {
        String accId = doc.getId();

        Long currentBalance = doc.getLong("balance");
        Double yearlyRate = doc.getDouble("interestRate");

        if (currentBalance == null || currentBalance <= 0 || yearlyRate == null) return;

        double rawInterest = (currentBalance * yearlyRate) / 12;

        long interestToPay = Math.round(rawInterest);

        if (interestToPay < 1) return;

        try {
            dbFirestore.runTransaction(t -> {
                DocumentReference accRef = dbFirestore.collection(COLLECTION_ACCOUNTS).document(accId);

                DocumentSnapshot latestAccSnap = t.get(accRef).get();
                if (!latestAccSnap.exists()) return null;

                Long latestBal = latestAccSnap.getLong("balance");
                if (latestBal == null) latestBal = 0L;

                String userId = latestAccSnap.getString("userId");
                String receiverName = "Unknown User";

                if (userId != null) {
                    DocumentReference userRef = dbFirestore.collection(COLLECTION_USERS).document(userId);
                    DocumentSnapshot userSnap = t.get(userRef).get();
                    if (userSnap.exists()) {
                        if (userSnap.contains("fullName")) {
                            receiverName = userSnap.getString("fullName");
                        } else if (userSnap.contains("name")) {
                            receiverName = userSnap.getString("name");
                        }
                    }
                }

                t.update(accRef, "balance", latestBal + interestToPay);

                DocumentReference transRef = dbFirestore.collection(COLLECTION_TRANSACTIONS).document();

                Transaction transaction = new Transaction();
                transaction.setTransactionId(transRef.getId());

                // Nguồn
                transaction.setSourceAcc("BANK_INTEREST_PAYOUT");
                transaction.setBankSourceSymbol("ATB");
                transaction.setSenderName("AstraBank System");

                // Đích
                transaction.setDestinationAcc(accId);
                transaction.setBankDesSymbol("ATB");
                transaction.setReceiverName(receiverName); // <-- Đã set tên thật lấy từ bảng Users

                // Số tiền v Nội dung
                transaction.setAmount(interestToPay);
                transaction.setType(TransactionType.PAYMENT);
                transaction.setDescription("Trả lãi tiết kiệm KKH tháng " + (Calendar.getInstance().get(Calendar.MONTH) + 1));

                // Trạng thái
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setCreatedAt(new Date());
                transaction.setUpdatedAt(new Date());

                t.set(transRef, transaction);

                return null;
            }).get();

            System.out.println(">> Đã trả lãi: " + interestToPay + " VND cho " + accId);

        } catch (Exception e) {
            System.err.println(">> Lỗi trả lãi TK " + accId + ": " + e.getMessage());
        }
    }
}