package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.constant.TransactionStatus;
import org.astrabank.dto.AccountResponse;
import org.astrabank.dto.TransactionRequest;
import org.astrabank.models.Transaction;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ExecutionException;

@Service
public class TransactionService {

    private final AccountService accountService;
    private final String BANK_DESTINATION_SYMBOL = "ATB";
    private final BankService bankService;

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
}
