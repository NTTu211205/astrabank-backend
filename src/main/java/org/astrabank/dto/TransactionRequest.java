package org.astrabank.dto;

import lombok.Data;
import org.astrabank.constant.TransactionType;

@Data
public class TransactionRequest {
    private String sourceAccountNumber;
    private String sourceBankSymbol;
    private String destinationAccountNumber;
    private String destinationBankSymbol;
    private long amount;
    private TransactionType transactionType;
    private String description;
    private String senderName;
    private String receiverName;

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "sourceAccountNumber='" + sourceAccountNumber + '\'' +
                ", sourceBankSymbol='" + sourceBankSymbol + '\'' +
                ", destinationAccountNumber='" + destinationAccountNumber + '\'' +
                ", destinationBankSymbol='" + destinationBankSymbol + '\'' +
                ", amount=" + amount +
                ", transactionType=" + transactionType +
                ", description='" + description + '\'' +
                ", senderName='" + senderName + '\'' +
                ", receiverName='" + receiverName + '\'' +
                '}';
    }
}
