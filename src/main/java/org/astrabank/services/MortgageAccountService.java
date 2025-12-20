package org.astrabank.services;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.constant.TransactionType;
import org.astrabank.dto.DisbursementRequest;
import org.astrabank.dto.LoanRequest;
import org.astrabank.dto.ReceiptPaymentRequest;
import org.astrabank.models.Loan;
import org.astrabank.models.Transaction;
import org.astrabank.models.LoanReceipt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class MortgageAccountService {

    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionService transactionService;

    public MortgageAccountService(PasswordEncoder passwordEncoder, TransactionService transactionService, AccountService accountService) {
        this.passwordEncoder = passwordEncoder;
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

//    public Loan createLoan(LoanRequest request) throws Exception {
//        Firestore dbFirestore = FirestoreClient.getFirestore();
//
//        // Validate cơ bản
//        if (request.getAmount() <= 0) throw new IllegalArgumentException("Số tiền vay phải lớn hơn 0");
//        if (request.getTerm() <= 0) throw new IllegalArgumentException("Kỳ hạn phải lớn hơn 0");
//
//        return dbFirestore.runTransaction(transaction -> {
//            // tim tai khoan the chap
//            System.out.println("Find mortgage account");
//            DocumentReference accRef = dbFirestore.collection("accounts").document(request.getAccountNumber());
//            DocumentSnapshot accSnap = transaction.get(accRef).get();
//            if (!accSnap.exists()) {
//                throw new IllegalArgumentException("Tài khoản không tồn tại");
//            }
//            if (request.getAmount() > accSnap.getLong("balance")) {
//                throw new Exception("Số tiền vay phải ít hơn hạn mức");
//            }
//
//            // kiem tra da co khoan vay chua
//            System.out.println("Checking mortgage account is loanning");
//            Boolean isLoan = accSnap.getBoolean("isLoan");
//            if (isLoan == null || isLoan) {
//                throw new IllegalArgumentException("Tài khoản đã có khoản vay chưa hoàn thành");
//            }
//
//            // tim so tai khoan thanh toan
//            System.out.println(accSnap.getString("userId"));
//            QuerySnapshot checkingQuery = transaction.get(
//                    dbFirestore.collection("accounts")
//                            .whereEqualTo("userId", accSnap.getString("userId"))
//                            .whereEqualTo("accountType", "CHECKING")
//                            .whereEqualTo("accountStatus", true) // Chỉ giải ngân vào TK đang hoạt động
//                            .limit(1)
//            ).get();
//
//            if (checkingQuery.isEmpty()) {
//                throw new IllegalArgumentException("Khách hàng này chưa có tài khoản thanh toán (Checking) hoạt động!");
//            }
//            DocumentSnapshot checkingDoc = checkingQuery.getDocuments().get(0);
//            String checkingAccountNumber = checkingDoc.getId();
//
//
//            // 2. KHỞI TẠO ĐỐI TƯỢNG LOAN
//            DocumentReference loanRef = dbFirestore.collection("loans").document();
//            String loanId = loanRef.getId();
//            Date now = new Date();
//
//            Loan loan = new Loan();
//            loan.setLoanId(loanId);
//            loan.setAccountNumber(request.getAccountNumber());
//            loan.setTerm(request.getTerm());
//            loan.setOriginalPrincipal(request.getAmount());
//            loan.setInterestRate(request.getInterestRate());
//            loan.setAddress(request.getAddress());
//
//            loan.setComplete(false);
//            loan.setCreatedAt(now);
//            loan.setDisbursementDate(now);
//            loan.setStatus("SUCCESS");
//
//            // Lưu Loan xuống DB
//            System.out.println("Save loan");
//            transaction.set(loanRef, loan);
//
//            // cap nhat tai khoan da co khoan vay
//            System.out.println("update mortgage account");
//            transaction.update(accRef, "isLoan", true);
//            transaction.update(accRef, "balance", FieldValue.increment(-request.getAmount()));
//
//            DocumentReference checkingAcc =  dbFirestore.collection("accounts").document(checkingAccountNumber);
//            transaction.update(checkingAcc, "balance", FieldValue.increment(request.getAmount()));
//
//            // tao request gui tien vao tai khoan checking
//            System.out.println("Disbursement checking account");
//            DocumentReference txRef = dbFirestore.collection("transactions").document();
//            Map<String, Object> txData = new HashMap<>();
//            txData.put("transactionId", txRef.getId());
//            txData.put("sourceBankSymbol", "ATB");
//            txData.put("bankDesSymbol", "ATB");
//            txData.put("sourceAcc", "SYSTEM_DISBURSEMENT");
//            txData.put("destinationAcc", checkingAccountNumber);
//            txData.put("status", "SUCCESS");
//            txData.put("amount", request.getAmount());
//            txData.put("type", "PAYMENT");
//            txData.put("description", "GIAI NGAN GIA KHOAN VAY");
//            txData.put("createdAt", new Date());
//            txData.put("updatedAt", new Date());
//            txData.put("senderName", "SYSTEM");
//            txData.put("receiverName", request.getName());
//            transaction.set(txRef, txData);
//
//            // tao lich thanh toan
//            System.out.println("create payment schedule");
//            generateLoanReceipts(transaction, loan.getLoanId(), loan.getOriginalPrincipal(), request.getTerm(), request.getInterestRate());
//
//            return loan;
//        }).get();
//    }

    private void generateLoanReceipts(com.google.cloud.firestore.Transaction t, String loanId, long amount, int term, double rate) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        long principalPerMonth = amount / term;     // Gốc phải trả hàng tháng (Chia đều)

        Date now = new Date();
        CollectionReference receiptCol = dbFirestore.collection("receipts");

        for (int i = 1; i <= term; i++) {
            long totalAmount = principalPerMonth + (long) (principalPerMonth * rate);

            DocumentReference receiptRef = receiptCol.document();
            Map<String, Object> receiptData = new HashMap<>();

            receiptData.put("receiptId", receiptRef.getId());
            receiptData.put("loanId", loanId);
            receiptData.put("paid", false);
            receiptData.put("period", i);
            receiptData.put("amount", totalAmount);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, i);
            int lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.DAY_OF_MONTH, Math.min(30, lastDayOfMonth));
            receiptData.put("finalDate", cal.getTime());
            receiptData.put("updatedAt", now);

            t.set(receiptRef, receiptData);
        }
    }

    public Loan createLoan(LoanRequest request) throws Exception {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        // Validate cơ bản
        if (request.getAmount() <= 0) throw new IllegalArgumentException("Số tiền vay phải lớn hơn 0");
        if (request.getTerm() <= 0) throw new IllegalArgumentException("Kỳ hạn phải lớn hơn 0");

        return dbFirestore.runTransaction(transaction -> {
            // tim tai khoan the chap
            System.out.println("Find mortgage account");
            DocumentReference accRef = dbFirestore.collection("accounts").document(request.getAccountNumber());
            DocumentSnapshot accSnap = transaction.get(accRef).get();
            if (!accSnap.exists()) {
                throw new IllegalArgumentException("Tài khoản không tồn tại");
            }
            if (request.getAmount() > accSnap.getLong("balance")) {
                throw new Exception("Số tiền vay phải ít hơn hạn mức");
            }

            // kiem tra da co khoan vay chua
            System.out.println("Checking mortgage account is loanning");
            Boolean isLoan = accSnap.getBoolean("isLoan");
            if (isLoan == null || isLoan) {
                throw new IllegalArgumentException("Tài khoản đã có khoản vay chưa hoàn thành");
            }

            // tim so tai khoan thanh toan
            System.out.println(accSnap.getString("userId"));
            QuerySnapshot checkingQuery = transaction.get(
                    dbFirestore.collection("accounts")
                            .whereEqualTo("userId", accSnap.getString("userId"))
                            .whereEqualTo("accountType", "CHECKING")
                            .whereEqualTo("accountStatus", true) // Chỉ giải ngân vào TK đang hoạt động
                            .limit(1)
            ).get();

            if (checkingQuery.isEmpty()) {
                throw new IllegalArgumentException("Khách hàng này chưa có tài khoản thanh toán (Checking) hoạt động!");
            }
            DocumentSnapshot checkingDoc = checkingQuery.getDocuments().get(0);
            String checkingAccountNumber = checkingDoc.getId();

            // 2. KHỞI TẠO ĐỐI TƯỢNG LOAN
            DocumentReference loanRef = dbFirestore.collection("loans").document();
            String loanId = loanRef.getId();
            Date now = new Date();

            Loan loan = new Loan();
            loan.setLoanId(loanId);
            loan.setAccountNumber(request.getAccountNumber());
            loan.setTerm(request.getTerm());
            loan.setOriginalPrincipal(request.getAmount());
            loan.setInterestRate(request.getInterestRate());
            loan.setAddress(request.getAddress());

            loan.setComplete(false);
            loan.setCreatedAt(now);
            loan.setDisbursementDate(now);
            loan.setStatus("SUCCESS");

            // Lưu Loan xuống DB
            System.out.println("Save loan");
            transaction.set(loanRef, loan);

            // cap nhat tai khoan da co khoan vay
            System.out.println("update mortgage account");
            transaction.update(accRef, "isLoan", true);
            transaction.update(accRef, "presentLoanId", loanId);
            accountService.deductBalanceTx(transaction, request.getAccountNumber(), request.getAmount());

            // tao request gui tien vao tai khoan checking
            System.out.println("Disbursement checking account");
            DisbursementRequest disbursementRequest = new DisbursementRequest();
            disbursementRequest.setReceiverName(request.getName());
            disbursementRequest.setAmount(request.getAmount());
            disbursementRequest.setTransactionType(TransactionType.PAYMENT);
            disbursementRequest.setDestinationBankSymbol("ATB");
            disbursementRequest.setDestinationAccountNumber(checkingAccountNumber);
            transactionService.processDisbursement(transaction, disbursementRequest);

            // tao lich thanh toan
            System.out.println("create payment schedule");
            generateLoanReceipts(transaction, loan.getLoanId(), loan.getOriginalPrincipal(), request.getTerm(), request.getInterestRate());
            return loan;
        }).get();
    }

    public Transaction pay(ReceiptPaymentRequest receiptPaymentRequest)  throws Exception  {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        return dbFirestore.runTransaction(transaction -> {
            // find checking account
            DocumentReference accRef = dbFirestore.collection("accounts").document(receiptPaymentRequest.getSourceAccountNumber());
            DocumentSnapshot accSnap = transaction.get(accRef).get();
            if (!accSnap.exists()) {
                throw new IllegalArgumentException("Checking account nout found");
            }
            if (!Objects.equals(accSnap.getString("accountType"), "CHECKING")) {
                throw new IllegalArgumentException("Account not allow to pay");
            }

            // find receipt
            DocumentReference receiptRef = dbFirestore.collection("receipts").document(receiptPaymentRequest.getReceiptId());
            DocumentSnapshot documentSnapshot =  transaction.get(receiptRef).get();
            LoanReceipt loanReceipt;
            if (!documentSnapshot.exists()) {
                throw new IllegalArgumentException("Receipt not found");
            }
            else {
                loanReceipt =  documentSnapshot.toObject(LoanReceipt.class);

                if (loanReceipt.isPaid()) {
                    throw new IllegalArgumentException("Payment was paid");
                }
            }

            // find loan
            DocumentReference loanRef =  dbFirestore.collection("loans").document(loanReceipt.getLoanId());
            DocumentSnapshot loanSnap = transaction.get(loanRef).get();
            if (!loanSnap.exists()) {
                throw new IllegalArgumentException("Loan not found");
            }

            // find mortgage account
            DocumentReference mortgageAccountRef = dbFirestore.collection("accounts").document(loanSnap.getString("accountNumber"));
            DocumentSnapshot mortgageAccountSnap = transaction.get(mortgageAccountRef).get();
            if (!mortgageAccountSnap.exists()) {
                throw new IllegalArgumentException("Mortgage account not found");
            }

            Transaction transaction1 = transactionService.processPaymentReceipt(transaction, receiptPaymentRequest, loanReceipt.getAmount(), "THANH TOAN HOA DON KI " + loanReceipt.getPeriod());

            transaction.update(mortgageAccountRef, "balance", FieldValue.increment(loanReceipt.getAmount()));

            transaction.update(receiptRef, "paid", true);
            transaction.update(receiptRef, "updatedAt", new Date());

            loanReceipt.setPaid(true);
            loanReceipt.setUpdatedAt(new Date());

            return transaction1;
        }).get();
    }

    public Loan getLoanById(String loanId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        // Tìm trực tiếp bằng Document ID
        DocumentSnapshot document = dbFirestore.collection("loans").document(loanId).get().get();

        if (document.exists()) {
            return document.toObject(Loan.class);
        } else {
            return null; // Trả về null nếu không tìm thấy
        }
    }

    public List<LoanReceipt> findReceiptsByLoanId(String loanId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection("receipts")
                .whereEqualTo("loanId", loanId);

        QuerySnapshot querySnapshot = query.get().get();

        return querySnapshot.toObjects(LoanReceipt.class);
    }
}
