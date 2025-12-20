package org.astrabank.dto;

import lombok.Data;
import org.astrabank.constant.TransactionType;

@Data
public class ReceiptPaymentRequest {
    private String receiptId;
    private String sourceAccountNumber;
    private String sourceBankSymbol;
    private String senderName;
}
