package org.astrabank.dto;

import lombok.Data;

@Data
public class AdminCustomerRequest {
    private String userID;
    private String fullName;
    private String dateOfBirth;
    private String nationalID;
    private String email;
    private String phone;
    private String address;
    private String occupation;
    private String companyName;
    private Double averageSalary;
    private String transactionPIN;
    private String createdBy;
    private String updatedBy;
    private long deposit;
}
