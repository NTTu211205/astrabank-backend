package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.astrabank.constant.AccountType;

import java.util.Date;
import java.util.concurrent.ExecutionException;

@Data
@AllArgsConstructor
public class MortgageAccount extends Account{
    private Boolean isLoan;
    private Double interestRate;
    private String presentLoanId;

    public MortgageAccount() {
        super();
    }

    public MortgageAccount(String userId, String accountNumber, boolean accountStatus,
                           long balance, AccountType accountType, Date createdAt,
                           Boolean isLoan, Double interestRate,  String presentLoanId) {
        super(userId, accountNumber, accountStatus, balance, accountType, createdAt);
        this.isLoan = isLoan;
        this.interestRate = interestRate;
        this.presentLoanId =  presentLoanId;
    }
}
