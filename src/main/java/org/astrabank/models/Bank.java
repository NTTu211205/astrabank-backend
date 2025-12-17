package org.astrabank.models;

import lombok.Data;

import java.util.Date;

@Data
public class Bank {
    private String bankName;
    private String bankSymbol;
    private String bankFullName;
    private Date createdAt;
    private Date updatedAt;
}
