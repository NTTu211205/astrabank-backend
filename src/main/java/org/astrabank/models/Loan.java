package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    private String loanId;
    private String accountNumber;
    private int term;
    private double interestRate;
    private boolean isComplete;
    private Date createdAt;
}
