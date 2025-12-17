package org.astrabank.controllers;

import com.google.protobuf.Api;
import org.astrabank.dto.AccountRequest;
import org.astrabank.dto.ApiResponse;
import org.astrabank.dto.BankRequest;
import org.astrabank.models.Account;
import org.astrabank.models.Bank;
import org.astrabank.services.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("api/banks")
public class BankController {
    private final int STATUS_CODE_FAILED = 400;
    private final int STATUS_CODE_OK = 200;
    private BankService bankService;

    @Autowired
    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Bank>> createBank(@RequestBody BankRequest bankRequest){
        try {
            Bank bank = bankService.createBank(bankRequest);

            if (bank != null) {
                ApiResponse<Bank> response = ApiResponse.<Bank>builder()
                        .code(STATUS_CODE_OK)
                        .message("Bank created successfully")
                        .result(bank)
                        .build();
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            else {
                ApiResponse<Bank> response = ApiResponse.<Bank>builder()
                        .code(STATUS_CODE_OK)
                        .message("Bank created not successfully")
                        .build();
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        }
        catch (Exception e) {
            ApiResponse<Bank> error = ApiResponse.<Bank>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Bank>>> getAllBanks(){
        try {
            List<Bank> banks = bankService.getAll();
            if (banks != null) {
                ApiResponse<List<Bank>> response = ApiResponse.<List<Bank>>builder()
                        .code(STATUS_CODE_OK)
                        .message("Find sucessfully")
                        .result(banks)
                        .build();
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            else {
                ApiResponse<List<Bank>> response = ApiResponse.<List<Bank>>builder()
                        .code(STATUS_CODE_OK)
                        .message("Not found")
                        .result(null)
                        .build();
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        }
        catch (Exception e) {
            ApiResponse<List<Bank>> response = ApiResponse.<List<Bank>>builder()
                    .code(STATUS_CODE_FAILED)
                    .message("Error")
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{bankSymbol}")
    public ResponseEntity<ApiResponse<Bank>>  getBank(@PathVariable("bankSymbol") String bankSymbol){
        try {
            Bank bank = bankService.getBank(bankSymbol);
            if (bank != null) {
                ApiResponse<Bank> response = ApiResponse.<Bank>builder()
                        .code(STATUS_CODE_OK)
                        .message("Bank found successfully")
                        .result(bank)
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            else {
                ApiResponse<Bank> response = ApiResponse.<Bank>builder()
                        .code(STATUS_CODE_OK)
                        .message("Bank not found")
                        .result(null)
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        }
        catch (Exception e) {
            ApiResponse<Bank> error = ApiResponse.<Bank>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
