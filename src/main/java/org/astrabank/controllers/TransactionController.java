package org.astrabank.controllers;

import org.astrabank.dto.ApiResponse;
import org.astrabank.dto.TransactionRequest;
import org.astrabank.models.Notification;
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

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Transaction>> deposit(@RequestBody TransactionRequest request) {
        try {
            // 1. Gọi Service xử lý nạp tiền
            Transaction transactionResult = transactionService.processDeposit(request);

            // 2. Trả về kết quả thành công
            ApiResponse<Transaction> response = ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_OK) // Mã thành công (quy ước của bạn)
                    .message("Nạp tiền thành công")
                    .result(transactionResult)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Lỗi do input sai (Số tiền âm, v.v.)
            return ResponseEntity.badRequest().body(ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build());

        } catch (Exception e) {
            // Lỗi hệ thống hoặc lỗi DB
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Transaction>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Transaction>> withdraw(@RequestBody TransactionRequest request) {
        try {
            // Gọi Service rút tiền
            Transaction result = transactionService.processWithdrawal(request);

            return ResponseEntity.ok(ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_OK)
                    .message("Rút tiền thành công")
                    .result(result)
                    .build());

        } catch (IllegalArgumentException e) {
            // Lỗi do User (Không đủ tiền, số âm...)
            return ResponseEntity.badRequest().body(ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .build());

        } catch (Exception e) {
            // Lỗi hệ thống
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Transaction>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi giao dịch: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getHistoryByUser(@PathVariable String userId) {
        try {
            List<Notification> transactions = transactionService.getAllTransactionsByUserId(userId);

            return ResponseEntity.ok(ApiResponse.<List<Notification>>builder()
                    .code(STATUS_CODE_OK)
                    .message("Lấy danh sách giao dịch thành công")
                    .result(transactions)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<Notification>>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi: " + e.getMessage())
                            .build());
        }
    }
}
