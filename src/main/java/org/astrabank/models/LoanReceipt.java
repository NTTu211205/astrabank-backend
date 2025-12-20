package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanReceipt {
    private String receiptId;
    private String loanId;
    private boolean paid;
    private int period;
    private long amount;
    private Date finalDate;
    private Date updatedAt;
}
