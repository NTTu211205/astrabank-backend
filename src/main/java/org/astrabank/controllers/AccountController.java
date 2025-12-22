package org.astrabank.controllers;

import org.astrabank.dto.*;
import org.astrabank.models.*;
import org.astrabank.services.AccountService;
import org.astrabank.services.MortgageAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("api/accounts")
public class AccountController {
    private final int STATUS_CODE_FAILED = 400;
    private final int STATUS_CODE_OK = 200;
    private final AccountService accountService;
    private final MortgageAccountService mortgageAccountService;
    private final PathPatternRequestMatcher.Builder builder;

    @Autowired
    public AccountController(AccountService accountService, PathPatternRequestMatcher.Builder builder, MortgageAccountService mortgageAccountService) {
        this.accountService = accountService;
        this.builder = builder;
        this.mortgageAccountService = mortgageAccountService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @RequestBody AccountRequest accountRequest) {
        try {
            Account account = accountService.createAccount(accountRequest);

            ApiResponse<Account> response = ApiResponse.<Account>builder()
                    .code(STATUS_CODE_OK)
                    .message("Account created successfully")
                    .result(account)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ApiResponse<Account> error = ApiResponse.<Account>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/create-saving-account")
    public ResponseEntity<ApiResponse<Account>> createSavingAccount(
            @RequestBody SavingAccountRequest accountRequest) {
        try {
            Account account = accountService.createSavingAccount(accountRequest);

            ApiResponse<Account> response = ApiResponse.<Account>builder()
                    .code(STATUS_CODE_OK)
                    .message("Account created successfully")
                    .result(account)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ApiResponse<Account> error = ApiResponse.<Account>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> findAccount(
            @PathVariable String accountNumber) {
        try {
            AccountResponse account = accountService.findAccount(accountNumber);

            ApiResponse<AccountResponse> response;
            if (account == null) {
                response = ApiResponse.<AccountResponse>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account not found")
                        .result(null)
                        .build();
            } else {
                response = ApiResponse.<AccountResponse>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account found successfully")
                        .result(account)
                        .build();
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            ApiResponse<AccountResponse> error = ApiResponse.<AccountResponse>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("my-account")
    public ResponseEntity<ApiResponse<Account>> getMyAccount(
            @RequestParam String userId,
            @RequestParam String accountType) {
        try {
            Account account = accountService.getAccountForUSer(userId, accountType);

            if (account == null) {
                ApiResponse<Account> response = ApiResponse.<Account>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account not found")
                        .result(null)
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                ApiResponse<Account> response = ApiResponse.<Account>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account found successfully")
                        .result(account)
                        .build();
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } catch (Exception e) {
            ApiResponse<Account> error = ApiResponse.<Account>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("findAll/{userId}")
    public ResponseEntity<ApiResponse<List<Account>>> getAllAccounts(@PathVariable String userId) {
        try {
            List<Account> accounts = accountService.findAllAccounts(userId);
            if (accounts == null) {
                ApiResponse<List<Account>> response = ApiResponse.<List<Account>>builder()
                        .code(STATUS_CODE_OK)
                        .result(null)
                        .message("Account not found")
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            else {
                ApiResponse<List<Account>> response = ApiResponse.<List<Account>>builder()
                        .code(STATUS_CODE_OK)
                        .result(accounts)
                        .message("Account found")
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } catch (Exception e) {
            ApiResponse<List<Account>> error = ApiResponse.<List<Account>>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // check stk different bank
    @GetMapping("/{accountNumber}/{bankSymbol}")
    public ResponseEntity<ApiResponse<AccountResponse>> findAccount(@PathVariable String accountNumber, @PathVariable String bankSymbol) {
        try {
            AccountResponse account = accountService.findAccount(accountNumber, bankSymbol);

            ApiResponse<AccountResponse> response;
            if (account == null) {
                response = ApiResponse.<AccountResponse>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account not found")
                        .result(null)
                        .build();
            } else {
                response = ApiResponse.<AccountResponse>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account found successfully")
                        .result(account)
                        .build();
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        catch (Exception e) {
            ApiResponse<AccountResponse> error = ApiResponse.<AccountResponse>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/create-mortgage-account")
    public ResponseEntity<ApiResponse<MortgageAccount>> openMortgage(@RequestBody MortgageAccountRequest request) {
        try {
            MortgageAccount account = accountService.createMortgageAccount(request);

            return ResponseEntity.ok(ApiResponse.<MortgageAccount>builder()
                    .code(STATUS_CODE_OK)
                    .message("Create successfully")
                    .result(account)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<MortgageAccount>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Create Failed: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("create-loan")
    public ResponseEntity<ApiResponse<Loan>> createLoan(@RequestBody LoanRequest request) throws ExecutionException, InterruptedException {
        try {
            Loan newLoan = mortgageAccountService.createLoan(request);

            return ResponseEntity.ok(ApiResponse.<Loan>builder()
                    .code(STATUS_CODE_OK)
                    .message("Đăng ký vay và giải ngân thành công!")
                    .result(newLoan)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Loan>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Loan>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @PostMapping("pay-receipt")
    public ResponseEntity<ApiResponse<Transaction>> payReceipt(@RequestBody ReceiptPaymentRequest request) {
        try {
            Transaction updatedReceipt = mortgageAccountService.pay(request);

            return ResponseEntity.ok(ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_OK)
                    .message("Thanh toán hóa đơn thành công!")
                    .result(updatedReceipt)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Transaction>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Transaction>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @GetMapping("/find-mortgage/{accountNumber}")
    public ResponseEntity<ApiResponse<Account>> getMortgageAccount(@PathVariable String accountNumber) {
        try {
            Account account = accountService.findMortgageAccount(accountNumber);

            if (account == null) {
                return ResponseEntity.ok(ApiResponse.<Account>builder()
                        .code(STATUS_CODE_OK)
                        .message("Account not found")
                        .result(null)
                        .build());
            }
            else {
                return ResponseEntity.ok(ApiResponse.<Account>builder()
                        .code(STATUS_CODE_OK)
                        .message("Lấy thông tin tài khoản thành công!")
                        .result(account)
                        .build());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Account>builder()
                            .code(STATUS_CODE_FAILED)
                            .message(e.getMessage())
                            .result(null)
                            .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Account>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @GetMapping("/find-loan/{loanId}")
    public ResponseEntity<ApiResponse<Loan>> getLoanById(@PathVariable String loanId) {
        try {
            Loan loan = mortgageAccountService.getLoanById(loanId);

            if (loan == null) {
                return ResponseEntity.ok(ApiResponse.<Loan>builder()
                        .code(STATUS_CODE_OK)
                        .message("Loan not found")
                        .result(loan)
                        .build());
            }
            else {
                return ResponseEntity.ok(ApiResponse.<Loan>builder()
                        .code(STATUS_CODE_OK)
                        .message("Found successfully loan")
                        .result(loan)
                        .build());
            }
        } catch (IllegalArgumentException e) {
            // Trả về 404 Not Found nếu ID sai
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Loan>builder()
                            .code(STATUS_CODE_FAILED)
                            .message(e.getMessage())
                            .result(null)
                            .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Loan>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Error: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @GetMapping("/find-receipt/{loanId}")
    public ResponseEntity<ApiResponse<List<LoanReceipt>>> getReceiptsByLoanId(@PathVariable String loanId) {
        try {
            List<LoanReceipt> receipts = mortgageAccountService.findReceiptsByLoanId(loanId);

            return ResponseEntity.ok(ApiResponse.<List<LoanReceipt>>builder()
                    .code(STATUS_CODE_OK)
                    .message("Find receipts successfully")
                    .result(receipts)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<LoanReceipt>>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Error: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @GetMapping("/find-receipt-by-id/{receiptId}")
    public ResponseEntity<ApiResponse<LoanReceipt>> findReceiptById(@PathVariable String receiptId) {
        try {
            LoanReceipt receipt = mortgageAccountService.findReceiptById(receiptId);

            if (receipt == null) {
                return ResponseEntity.ok(ApiResponse.<LoanReceipt>builder()
                        .code(STATUS_CODE_OK)
                        .message("Receipt not found")
                        .result(null)
                        .build());
            }
            else {
                return ResponseEntity.ok(ApiResponse.<LoanReceipt>builder()
                        .code(STATUS_CODE_OK)
                        .message("Find receipt successfully ")
                        .result(receipt)
                        .build());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<LoanReceipt>builder()
                            .code(STATUS_CODE_FAILED)
                            .message(e.getMessage())
                            .result(null)
                            .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<LoanReceipt>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @GetMapping("/mortgages")
    public ResponseEntity<ApiResponse<List<MortgageAccount>>> getAllMortgageAccounts() {
        try {
            List<MortgageAccount> accounts = mortgageAccountService.getMortgageAccounts();

             if (accounts == null) {
                 return ResponseEntity.ok(ApiResponse.<List<MortgageAccount>>builder()
                         .code(STATUS_CODE_OK)
                         .message("not found")
                         .result(null)
                         .build());
             }
             else {
                 return ResponseEntity.ok(ApiResponse.<List<MortgageAccount>>builder()
                         .code(STATUS_CODE_OK)
                         .message("Find all mortgage accounts")
                         .result(accounts)
                         .build());
             }

        } catch (Exception e) {
            return ResponseEntity.status(STATUS_CODE_FAILED)
                    .body(ApiResponse.<List<MortgageAccount>>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(null)
                            .build());
        }
    }

    @PutMapping("/update-status/{accountNumber}/{status}")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(
            @PathVariable String accountNumber,
            @PathVariable Boolean status) {
        try {

            boolean isUpdated = accountService.updateAccountStatus(accountNumber, status);

            if  (isUpdated) {
                return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                        .code(STATUS_CODE_OK)
                        .message("Update successfully")
                        .result(isUpdated)
                        .build());
            }
            else {
                return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                        .code(STATUS_CODE_OK)
                        .message("update failed")
                        .result(isUpdated)
                        .build());
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Boolean>builder()
                            .code(STATUS_CODE_FAILED)
                            .message(e.getMessage())
                            .result(false)
                            .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Boolean>builder()
                            .code(STATUS_CODE_FAILED)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .result(false)
                            .build());
        }
    }
}
