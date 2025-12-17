package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.astrabank.constant.TransactionStatus;
import org.astrabank.constant.TransactionType;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    private String transactionId;               // Mã giao dịch
    private String sourceAcc;                   // Tài khoản nguồn
    private String bankSourceSymbol;
    private String destinationAcc;              // Tài khoản đích
    private String bankDesSymbol;
    private TransactionStatus status;           // Trạng thái giao dịch (success, pending, failed)
    private long amount;                        // Số tiền giao dịch
    private TransactionType type;               // Loại giao dịch (transfer, deposit, withdraw,...)
    private String description;                 // Mô tả giao dịch
    private Date createdAt;
    private Date updatedAt;
    private String senderName;
    private String receiverName;
}
