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
    private String isPay;
    private Date finalDate;
    private Date updatedAt;
}
