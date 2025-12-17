package org.astrabank.controllers;

import org.astrabank.dto.ApiResponse;
import org.astrabank.dto.TransactionRequest;
import org.astrabank.models.Transaction;
import org.astrabank.services.AccountService;
import org.astrabank.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/transactions")
public class TransactionController {
    private final int STATUS_CODE_FAILED = 400;
    private final int STATUS_CODE_OK = 200;
    private TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transferMoney(@RequestBody TransactionRequest request) {
        try {
            Transaction result = transactionService.progressTransferring(request);

            ApiResponse<Transaction> response = ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_OK)
                    .message("Giao dịch thành công")
                    .result(result)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            ApiResponse<Transaction> error = ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            e.printStackTrace();

            ApiResponse<Transaction> error = ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_FAILED)
                    .message("Giao dịch thất bại: " + e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
