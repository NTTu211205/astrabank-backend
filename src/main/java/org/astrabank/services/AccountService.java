package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.constant.AccountType;
import org.astrabank.constant.TransactionType;
import org.astrabank.dto.AccountRequest;
import org.astrabank.dto.AccountResponse;
import org.astrabank.dto.MortgageAccountRequest;
import org.astrabank.dto.SavingAccountRequest;
import org.astrabank.models.Account;
import org.astrabank.models.Bank;
import org.astrabank.models.MortgageAccount;
import org.astrabank.models.User;
import org.astrabank.utils.AccountNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class AccountService {
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();

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

        if (accountRequest.getAccountType().toString().equals("SAVING")) {
            account.setInterestRate(0.045);
        }

        ApiFuture<WriteResult> future = dbFirestore.collection("accounts").document(accountNumber).set(account);

        return account;
    }

    public Account createAccount(AccountRequest accountRequest, long balance) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        String accountNumber = generateUniqueAccountNumber(dbFirestore);

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountStatus(true);
        account.setAccountType(accountRequest.getAccountType());
        account.setBalance(balance);
        account.setCreatedAt(new Date());
        account.setUserId(accountRequest.getUserId());

        if (accountRequest.getAccountType().toString().equals("SAVING")) {
            account.setInterestRate(0.045);
        }

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

    public AccountResponse findAccount(String accountNumber, String bankSymbol) throws Exception {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        DocumentSnapshot documentSnapshot = dbFirestore.collection("banks").document(bankSymbol).get().get();

        if (documentSnapshot.exists()) {
            Bank bank = documentSnapshot.toObject(Bank.class);

            if (bank != null) {
                String link = bank.getLinkApiCheck() + accountNumber;
                Object result = callUrl(link, null);

                Map<String, Object> resultMap = (Map<String, Object>) result;
                AccountResponse accountResponse = new AccountResponse();
                accountResponse.setAccountNumber(accountNumber);
                accountResponse.setAccountName((String) resultMap.get("fullName"));
                return accountResponse;
            }
            else {
                throw new IllegalArgumentException("Bank not found");
            }
        }
        else {
            throw new IllegalArgumentException("Bank not found");
        }
    }

    public Object callUrl(String url, Object requestBody) throws Exception {
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("URL không hợp lệ");
        }

        if (requestBody != null) {
            // Tạo Header (thường là JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

            return restTemplate.postForObject(url, entity, Object.class);
        }

        else {
            return restTemplate.getForObject(url, Object.class);
        }
    }

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

        List<Account> accounts = new ArrayList<>();
        if (!querySnapshot.isEmpty()) {
            for (QueryDocumentSnapshot document : querySnapshot) {
                Account account = document.toObject(Account.class);
                if (!AccountType.MORTGAGE.equals(account.getAccountType())) {
                    accounts.add(account);
                }
            }
            return accounts;
        }
        else {
            throw new IllegalArgumentException("User not found");
        }
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

    public Account createSavingAccount(SavingAccountRequest accountRequest) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        String accountNumber = generateUniqueAccountNumber(dbFirestore);

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountStatus(true);
        account.setAccountType(accountRequest.getAccountType());
        account.setBalance(accountRequest.getBalance());
        account.setCreatedAt(new Date());
        account.setUserId(accountRequest.getUserId());
        account.setInterestRate(0.045);

        ApiFuture<WriteResult> future = dbFirestore.collection("accounts").document(accountNumber).set(account);

        return account;
    }

    public MortgageAccount createMortgageAccount(MortgageAccountRequest request) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        String newAccountNumber = generateUniqueAccountNumber(dbFirestore);

        MortgageAccount newAccount = new MortgageAccount();

        // --- Set thuộc tính chung của Account ---
        newAccount.setUserId(request.getUserId());
        newAccount.setAccountNumber(newAccountNumber);
        newAccount.setBalance(request.getBalance());
        newAccount.setAccountType(AccountType.MORTGAGE);
        newAccount.setAccountStatus(true);
        newAccount.setCreatedAt(new Date());
        newAccount.setInterestRate(request.getInterestRate());
        newAccount.setIsLoan(false);
        newAccount.setPresentLoanId("");

        dbFirestore.collection("accounts").document(newAccountNumber).set(newAccount).get();

        return newAccount;
    }

    public MortgageAccount findMortgageAccount(String accountNumber) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection("accounts").document(accountNumber);
        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            return document.toObject(MortgageAccount.class);
        } else {
            return null;
        }
    }

    public boolean updateAccountStatus(String accountNumber, Boolean newStatus) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // --- BƯỚC 1: KIỂM TRA ĐIỀU KIỆN NGHIỆP VỤ ---
        // Chỉ kiểm tra khi hành động là "Khóa tài khoản" (newStatus = false)
        if (Boolean.FALSE.equals(newStatus)) {

            Query incompleteLoanQuery = db.collection("loans")
                    .whereEqualTo("accountNumber", accountNumber)
                    .whereEqualTo("complete", false)
                    .limit(1);

            QuerySnapshot loanSnapshot = incompleteLoanQuery.get().get();

            if (!loanSnapshot.isEmpty()) {
                throw new IllegalArgumentException("Account has incomplete loan");
            }
        }

        Query accountQuery = db.collection("accounts")
                .whereEqualTo("accountNumber", accountNumber)
                .limit(1);

        QuerySnapshot accountSnapshot = accountQuery.get().get();

        if (accountSnapshot.isEmpty()) {
            throw new IllegalArgumentException("Account Number " + accountNumber + " not found");
        }

        DocumentReference docRef = accountSnapshot.getDocuments().get(0).getReference();

        docRef.update("accountStatus", newStatus).get();

        return true;
    }
}
