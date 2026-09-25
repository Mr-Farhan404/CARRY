package com.carry.controller;

import com.carry.dto.AuthResponse;
import com.carry.dto.LoginRequest;
import com.carry.dto.RegisterRequest;
import com.carry.service.AuthService;
import com.carry.exception.BadRequestException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    
    // Simple in-memory rate limiting for /login
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long TIME_WINDOW_MS = 60000; // 1 minute

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        checkRateLimit(ip);
        
        try {
            AuthResponse response = authService.login(request);
            loginAttempts.remove(ip); // clear on success
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            recordFailedAttempt(ip);
            throw e; // throw original exception (e.g., UnauthorizedException)
        }
    }
    
    private void checkRateLimit(String ip) {
        LoginAttempt attempt = loginAttempts.get(ip);
        if (attempt != null) {
            if (Instant.now().toEpochMilli() - attempt.firstAttemptTime > TIME_WINDOW_MS) {
                loginAttempts.remove(ip); // window expired, reset
            } else if (attempt.count >= MAX_ATTEMPTS) {
                // Using 429 Too Many Requests would be ideal, but BadRequestException maps to 400
                // For simplicity, we throw a runtime exception that mapped to 429 ideally, 
                // but let's use a custom or BadRequestException with a clear message.
                throw new com.carry.exception.TooManyRequestsException("Too many login attempts. Please try again later.");
            }
        }
    }
    
    private void recordFailedAttempt(String ip) {
        loginAttempts.compute(ip, (k, v) -> {
            if (v == null || Instant.now().toEpochMilli() - v.firstAttemptTime > TIME_WINDOW_MS) {
                return new LoginAttempt(1, Instant.now().toEpochMilli());
            }
            v.count++;
            return v;
        });
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    
    private static class LoginAttempt {
        int count;
        long firstAttemptTime;
        
        LoginAttempt(int count, long firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
    }
}
