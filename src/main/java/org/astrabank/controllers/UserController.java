package org.astrabank.controllers;

import org.astrabank.dto.ApiResponse;
import org.astrabank.dto.FirebaseLoginRequest;
import org.astrabank.dto.LoginRequest;
import org.astrabank.models.User;
import org.astrabank.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("api/users")
public class UserController {
    private final int STATUS_CODE_FAILED = 400;
    private final int STATUS_CODE_OK = 200;
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        try {
            // Gọi Service
            User createdUser = userService.createUser(user);

            // Tạo cấu trúc phản hồi chuẩn
            ApiResponse<User> response = ApiResponse.<User>builder()
                    .code(STATUS_CODE_OK) // Quy ước: 1000 là thành công
                    .message("Registered successfully")
                    .result(createdUser)
                    .build();

            // Trả về HTTP 201 (Created) kèm body
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        }
        catch (IllegalArgumentException e) {
            ApiResponse<User> errorResponse = ApiResponse.<User>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        catch (Exception e) {
            ApiResponse<User> errorResponse = ApiResponse.<User>builder()
                    .code(STATUS_CODE_FAILED)
                    .message(e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/check-existence")
    public ResponseEntity<ApiResponse<Boolean>> checkExistence(
            @RequestParam String email,
            @RequestParam String phoneNumber) {
        try {
            // Gọi Service kiểm tra
            boolean exists = userService.checkUserExists(email, phoneNumber);

            ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                    .code(STATUS_CODE_OK)
                    .message(exists ? "Data not valid" : "Data valid")
                    .result(exists)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            ApiResponse<Boolean> error = ApiResponse.<Boolean>builder()
                    .code(STATUS_CODE_FAILED)
                    .message("Checking eror: " + e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/login-with-phone")
    public ResponseEntity<ApiResponse<User>> loginWithPhone(@RequestBody FirebaseLoginRequest firebaseLoginRequest) {
        try {
            User user = userService.loginWithPhone(firebaseLoginRequest.getIdToken());

            ApiResponse<User> response = ApiResponse.<User>builder()
                    .code(STATUS_CODE_OK)
                    .message("Login successfully")
                    .result(user)
                    .build();
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        catch (Exception e) {
            ApiResponse<User> error = ApiResponse.<User>builder()
                    .code(STATUS_CODE_FAILED)
                    .message("Login failed: " + e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

            if (user != null) {
                ApiResponse<User> response = ApiResponse.<User>builder()
                        .code(STATUS_CODE_OK)
                        .message("Login successfully")
                        .result(user)
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
            else {
                ApiResponse<User> error = ApiResponse.<User>builder()
                        .code(STATUS_CODE_OK)
                        .message("Email or password is invalid")
                        .result(null)
                        .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
        }
        catch (Exception e) {
            ApiResponse<User> error = ApiResponse.<User>builder()
                    .code(STATUS_CODE_FAILED)
                    .message("Login failed: " + e.getMessage())
                    .result(null)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/validate-transferring")
    public ResponseEntity<ApiResponse<Boolean>> validateTransferring(
            @RequestParam String userId,
            @RequestParam String transactionPIN) {
        try {
            Boolean result = userService.checkTransactionPIN(transactionPIN, userId);

            ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                    .code(STATUS_CODE_OK)
                    .message("validate successfully")
                    .result(result)
                    .build();
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            ApiResponse<Boolean> error = ApiResponse.<Boolean>builder()
                    .code(STATUS_CODE_FAILED)
                    .message("validate failed: " + e.getMessage())
                    .result(null)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
