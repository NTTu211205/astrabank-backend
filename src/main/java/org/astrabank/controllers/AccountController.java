package org.astrabank.controllers;

import org.astrabank.dto.AccountRequest;
import org.astrabank.dto.AccountResponse;
import org.astrabank.dto.ApiResponse;
import org.astrabank.dto.SavingAccountRequest;
import org.astrabank.models.Account;
import org.astrabank.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/accounts")
public class AccountController {
    private final int STATUS_CODE_FAILED = 400;
    private final int STATUS_CODE_OK = 200;
    private final AccountService accountService;
    private final PathPatternRequestMatcher.Builder builder;

    @Autowired
    public AccountController(AccountService accountService, PathPatternRequestMatcher.Builder builder) {
        this.accountService = accountService;
        this.builder = builder;
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
            @RequestParam String accountType
    ) {
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
}
