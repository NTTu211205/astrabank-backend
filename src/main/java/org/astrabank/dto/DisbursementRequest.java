package org.astrabank.dto;

import lombok.Data;
import org.astrabank.constant.TransactionType;

@Data
public class DisbursementRequest {
    private String destinationAccountNumber;
    private String destinationBankSymbol;
    private long amount;
    private TransactionType transactionType;
    private String receiverName;
}
