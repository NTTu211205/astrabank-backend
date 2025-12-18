package org.astrabank.dto;

import lombok.Data;
import org.astrabank.constant.AccountType;

@Data
public class SavingAccountRequest {
    private String userId;           // ID người sở hữu tài khoản
    private AccountType accountType;        // Loại tài khoản (saving, checking,...)
    private Long balance;
}
