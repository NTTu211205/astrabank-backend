package org.astrabank.models;

import lombok.Data;

import java.util.Date;

@Data
public class Staff {
    private String userID;
    private String fullName;
    private String dateOfBirth;
    private String nationalID;
    private String email;
    private String phone;
    private String address;
    private String transactionPIN;
    private Boolean status;
    private String role;
    private Date createdAt;
    private Date updatedAt;
    private String updateBy;
    private String createdBy;
}
