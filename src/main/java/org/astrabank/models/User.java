package org.astrabank.models;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
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
    private Boolean status;
    private String transactionPIN;
    private Date createdAt;
    private Date updatedAt;
    private String password;
}
