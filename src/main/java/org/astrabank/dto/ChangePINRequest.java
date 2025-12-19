package org.astrabank.dto;

import lombok.Data;

@Data
public class ChangePINRequest {
    private String userId;
    private String oldPin;
    private String newPin;
    private String confirmNewPin;
}
