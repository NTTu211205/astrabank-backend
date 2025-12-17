package org.astrabank.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.astrabank.constant.AccountType;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String userId;           // ID người sở hữu tài khoản
    private String accountNumber;    // Số tài khoản
    private boolean AccountStatus;          // Trạng thái tài khoản (active, disabled,...)
    private long balance;            // Số dư
    private AccountType accountType;        // Loại tài khoản (saving, checking,...)
    private Date createdAt;
}
