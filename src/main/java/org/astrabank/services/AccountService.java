package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.dto.AccountRequest;
import org.astrabank.dto.AccountResponse;
import org.astrabank.models.Account;
import org.astrabank.models.Bank;
import org.astrabank.models.User;
import org.astrabank.utils.AccountNumberGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class AccountService {
    private final PasswordEncoder passwordEncoder;

    public AccountService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Account createAccount(AccountRequest accountRequest) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        String accountNumber = generateUniqueAccountNumber(dbFirestore);

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountStatus(true);
        account.setAccountType(accountRequest.getAccountType());
        account.setBalance(0);
        account.setCreatedAt(new Date());
        account.setUserId(accountRequest.getUserId());

        ApiFuture<WriteResult> future = dbFirestore.collection("accounts").document(accountNumber).set(account);

        return account;
    }

    private String generateUniqueAccountNumber(Firestore db) throws ExecutionException, InterruptedException {
        String accountNumber;
        boolean exists;

        do {
            accountNumber = AccountNumberGenerator.generate();

            QuerySnapshot query = db.collection("accounts")
                    .whereEqualTo("accountNumber", accountNumber)
                    .limit(1)
                    .get().get();

            exists = !query.isEmpty();

        } while (exists);

        return accountNumber;
    }

    public AccountResponse findAccount(String accountNumber) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        DocumentSnapshot documentSnapshot = dbFirestore.collection("accounts").document(accountNumber).get().get();

        if (documentSnapshot.exists()) {
            Account account = documentSnapshot.toObject(Account.class);

            if (account != null) {
                System.out.println(account.getAccountNumber());
                DocumentSnapshot doc = dbFirestore.collection("users").document(account.getUserId()).get().get();

                if (doc.exists()) {
                    User user = doc.toObject(User.class);

                    if (user != null) {
                        System.out.println(user.getFullName());
                        AccountResponse accountResponse = new AccountResponse();
                        accountResponse.setAccountNumber(account.getAccountNumber());
                        accountResponse.setAccountName(user.getFullName());
                        return accountResponse;
                    }
                }
            }
        }

        return null;
    }

//    public AccountResponse findAccount(String accountNumber, String bankSymbol) throws ExecutionException, InterruptedException {
//        Firestore dbFirestore = FirestoreClient.getFirestore();
//
//        DocumentSnapshot documentSnapshot = dbFirestore.collection("accounts").document(bankSymbol).get().get();
//
//        if (documentSnapshot.exists()) {
//            Bank bank = documentSnapshot.toObject(Bank.class);
//
//            if (bank != null) {
//
//            }
//            else {
//                throw new IllegalArgumentException("Bank not found");
//            }
//        }
//        else {
//            throw new IllegalArgumentException("Bank not found");
//        }
//    }

    public Account getAccountForUSer(String userId, String accountType)
            throws ExecutionException, InterruptedException, IllegalArgumentException {
        Firestore dbFirestore = FirestoreClient.getFirestore();


        QuerySnapshot querySnapshot = dbFirestore.collection("accounts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("accountType", accountType)
                .limit(1)
                .get().get();

        if (!querySnapshot.isEmpty()) {
            Account account = querySnapshot.getDocuments().get(0).toObject(Account.class);
            return account;
        }

        return null;
    }

    public Boolean checkBalance(String accountNumber, long amount)
            throws ExecutionException, InterruptedException, NullPointerException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        DocumentSnapshot documentSnapshot = dbFirestore.collection("accounts").document(accountNumber).get().get();
        if (documentSnapshot.exists()) {
            Account account = documentSnapshot.toObject(Account.class);

            return account != null && account.getBalance() >= amount;
        }
        else {
            return false;
        }
    }

    public List<Account> findAllAccounts(String userId) throws ExecutionException, InterruptedException, NullPointerException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        QuerySnapshot querySnapshot = dbFirestore.collection("accounts")
                .whereEqualTo("userId", userId)
                .get().get();

        if (!querySnapshot.isEmpty()) {
            for (QueryDocumentSnapshot document : querySnapshot) {
                Account account = document.toObject(Account.class);
                return Collections.singletonList(account);
            }
        }
        else {
            throw new IllegalArgumentException("User not found");
        }
        return null;
    }

    public ApiFuture<WriteResult> subtractBalance(String accountNumber, double amount) {
        Firestore db = FirestoreClient.getFirestore();

        // Dùng FieldValue.increment số âm để trừ
        return db.collection("accounts")
                .document(accountNumber)
                .update("balance", FieldValue.increment(-amount));
    }

    public ApiFuture<WriteResult> addBalance(String accountNumber, double amount) {
        Firestore db = FirestoreClient.getFirestore();

        // Dùng FieldValue.increment số dương để cộng
        return db.collection("accounts")
                .document(accountNumber)
                .update("balance", FieldValue.increment(amount));
    }

    public void deductBalanceTx(com.google.cloud.firestore.Transaction t, String accountNumber, double amount) {
        DocumentReference ref = FirestoreClient.getFirestore().collection("accounts").document(accountNumber);
        // Lưu ý: Dùng 't.update' thay vì 'ref.update'
        t.update(ref, "balance", FieldValue.increment(-amount));
    }

    public void addBalanceTx(com.google.cloud.firestore.Transaction t, String accountNumber, double amount) {
        DocumentReference ref = FirestoreClient.getFirestore().collection("accounts").document(accountNumber);
        t.update(ref, "balance", FieldValue.increment(amount));
    }
}
