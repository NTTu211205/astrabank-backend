package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.astrabank.constant.AccountType;

import java.util.Date;
import java.util.concurrent.ExecutionException;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MortgageAccount extends Account{
    private Boolean isLoan;
    private Double interestRate;
    private int term;
    private Date disbursementDate;

    public MortgageAccount(String userId, String accountNumber, boolean accountStatus,
                           long balance, AccountType accountType, Date createdAt,
                           Boolean isLoan, Double interestRate) {
        super(userId, accountNumber, accountStatus, balance, accountType, createdAt);
        this.isLoan = isLoan;
        this.interestRate = interestRate;
    }
}
