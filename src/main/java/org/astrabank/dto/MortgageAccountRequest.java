package org.astrabank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.astrabank.constant.AccountType;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MortgageAccountRequest {
    private String userId;           // ID người sở hữu tài khoản
}
