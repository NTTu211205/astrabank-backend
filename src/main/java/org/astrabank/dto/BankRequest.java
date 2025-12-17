package org.astrabank.dto;

import lombok.Data;

@Data
public class BankRequest {
    private String bankName;
    private String bankSymbol;
    private String bankFullName;
}
