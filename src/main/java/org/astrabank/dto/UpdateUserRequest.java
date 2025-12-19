package org.astrabank.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String dateOfBirth;
    private String nationalID;
    private String email;
    private String phone;
    private String address;
    private String occupation;
    private String companyName;
    private Double averageSalary;
}
