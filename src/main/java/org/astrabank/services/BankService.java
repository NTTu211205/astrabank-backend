package org.astrabank.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.astrabank.dto.BankRequest;
import org.astrabank.models.Bank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class BankService {
    private final PasswordEncoder passwordEncoder;

    public BankService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Bank createBank(BankRequest bankRequest) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        Bank bank = new Bank();
        bank.setBankName(bankRequest.getBankName());
        bank.setBankSymbol(bankRequest.getBankSymbol());
        bank.setBankFullName(bankRequest.getBankFullName());
        bank.setCreatedAt(new Date());
        bank.setUpdatedAt(new Date());

        dbFirestore.collection("banks").document(bank.getBankSymbol()).set(bank);
        return bank;
    }

    public boolean checkBank(String bankSymbol)  throws ExecutionException, InterruptedException  {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        DocumentSnapshot documentSnapshot = dbFirestore.collection("banks").document(bankSymbol).get().get();

        if (documentSnapshot.exists()) {
            Bank bank = documentSnapshot.toObject(Bank.class);

            if (bank != null) {
                return true;
            }
        }
        return false;
    }

    public List<Bank> getAll() throws ExecutionException, InterruptedException   {
        Firestore dbFirestore = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = dbFirestore.collection("banks").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Bank> banks = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Bank bank = document.toObject(Bank.class);
            bank.setCreatedAt(null);
            bank.setUpdatedAt(null);
            banks.add(bank);
        }

        return banks;
    }
}
