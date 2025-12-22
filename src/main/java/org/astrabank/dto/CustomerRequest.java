package org.astrabank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class CustomerRequest {
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
}
