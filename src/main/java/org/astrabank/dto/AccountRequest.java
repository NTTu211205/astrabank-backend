package org.astrabank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.astrabank.constant.AccountType;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequest {
    private String userId;           // ID người sở hữu tài khoản
    private AccountType accountType;        // Loại tài khoản (saving, checking,...)
}
