package org.astrabank.controllers;

import org.astrabank.dto.ApiResponse;
import org.astrabank.dto.TransactionRequest;
import org.astrabank.models.Transaction;
import org.astrabank.services.AccountService;
import org.astrabank.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/receiveTransaction")
    public ResponseEntity<ApiResponse<Transaction>> receiveTransaction(@RequestBody TransactionRequest request) {
        try {
            Transaction result = transactionService.receiveTransfer(request);

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

    @PostMapping("/sendTransaction")
    public ResponseEntity<ApiResponse<Transaction>> sendTransaction(@RequestBody TransactionRequest request) {
        try {
            Transaction result = transactionService.sendTransaction(request);

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

    @GetMapping("/histories/{accountNumber}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistory(@PathVariable String accountNumber) {
        try {
            List<Transaction> history = transactionService.getTransactionHistory(accountNumber);

            return ResponseEntity.ok(ApiResponse.<List<Transaction>>builder()
                    .code(STATUS_CODE_OK)
                    .message("Get histories successfully")
                    .result(history)
                    .build());
        }
        catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<Transaction>>builder()
                    .code(STATUS_CODE_OK)
                    .message("Get transaction history failed: " + e.getMessage())
                    .result(null)
                    .build());
        }
    }

}
