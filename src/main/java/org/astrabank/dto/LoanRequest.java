package org.astrabank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequest {
    private String accountNumber;
    private int term;
    private double interestRate;
    private String address;
    private long amount;
    private String name;
}
