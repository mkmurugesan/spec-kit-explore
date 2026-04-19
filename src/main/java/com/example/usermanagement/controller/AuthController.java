package com.example.usermanagement.controller;

import com.example.usermanagement.dto.*;
import com.example.usermanagement.service.PasswordResetService;
import com.example.usermanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Value("${app.env:dev}")
    private String appEnv;

    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = userService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest request) {
        SigninResponse response = userService.signin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, Object>> requestReset(@Valid @RequestBody PasswordResetRequestDto dto) {
        String token = passwordResetService.requestReset(dto.email());
        Map<String, Object> body = new HashMap<>();
        body.put("message", "If the email is registered, a password reset token has been generated.");
        if ("dev".equals(appEnv) && token != null) {
            body.put("resetToken", token);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, String>> confirmReset(@Valid @RequestBody PasswordResetConfirmDto dto) {
        passwordResetService.confirmReset(dto.token(), dto.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}

