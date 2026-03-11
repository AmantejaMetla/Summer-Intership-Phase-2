package com.eshop.auth.controller;

import com.eshop.auth.entity.Role;
import com.eshop.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/roles")
    public List<Role> listRoles() {
        return authService.listAllRoles();
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @RequestBody Map<String, Long> body) {
        Long roleId = body != null ? body.get("roleId") : null;
        if (roleId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "roleId required"));
        }
        return authService.assignRoleToUser(userId, roleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password(), request.totpCode())
                .map(result -> ResponseEntity.ok(Map.of(
                        "userId", result.userId(),
                        "accessToken", result.accessToken(),
                        "refreshToken", result.refreshToken(),
                        "tokenType", "Bearer",
                        "roles", result.roles()
                )))
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials, email not verified, or missing/invalid 2FA code")));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return authService.registerInit(request.email(), request.password(), request.otpChannel(), request.phoneNumber())
                .map(result -> ResponseEntity.ok(Map.of(
                        "userId", result.userId(),
                        "email", result.email(),
                        "nextStep", result.nextStep(),
                        "message", "Registration started. Verify OTP, then setup Google Authenticator."
                )))
                .orElse(ResponseEntity.status(400).body(Map.of("error", "Invalid input. For SMS OTP, provide a valid Indian mobile number.")));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and otp required"));
        }
        boolean ok = authService.verifyEmailOtp(email, otp);
        if (!ok) return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired OTP"));
        return ResponseEntity.ok(Map.of("message", "Email verified", "nextStep", "SETUP_TOTP"));
    }

    @PostMapping("/totp/setup")
    public ResponseEntity<?> setupTotp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password required"));
        }
        return authService.setupTotp(email, password)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(Map.of(
                        "userId", r.userId(),
                        "secret", r.secret(),
                        "otpAuthUri", r.otpAuthUri()
                )))
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Unable to setup TOTP. Verify email and credentials.")));
    }

    @PostMapping("/totp/confirm")
    public ResponseEntity<?> confirmTotp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otpCode = body.get("otpCode");
        if (email == null || otpCode == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and otpCode required"));
        }
        boolean ok = authService.confirmTotp(email, otpCode);
        if (!ok) return ResponseEntity.status(400).body(Map.of("error", "Invalid TOTP code"));
        return ResponseEntity.ok(Map.of("message", "2FA enabled. You can now login."));
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> forgotPasswordRequest(@RequestBody ForgotPasswordRequest request) {
        if (request.email() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email required"));
        }
        boolean requested = authService.requestForgotPasswordOtp(request.email(), request.otpChannel(), request.phoneNumber());
        if (!requested) {
            return ResponseEntity.status(404).body(Map.of("error", "Email is not registered or SMS phone does not match"));
        }
        return ResponseEntity.ok(Map.of("message", "OTP dispatch initiated."));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> forgotPasswordVerifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and otp required"));
        }
        return authService.verifyForgotPasswordOtp(email, otp)
                .<ResponseEntity<?>>map(token -> ResponseEntity.ok(Map.of(
                        "resetToken", token,
                        "nextStep", "RESET_WITH_TOTP"
                )))
                .orElse(ResponseEntity.status(400).body(Map.of("error", "Invalid or expired OTP")));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> forgotPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String resetToken = body.get("resetToken");
        String newPassword = body.get("newPassword");
        String totpCode = body.get("totpCode");
        if (email == null || resetToken == null || newPassword == null || totpCode == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email, resetToken, newPassword, totpCode required"));
        }
        boolean ok = authService.resetPasswordWith2fa(email, resetToken, newPassword, totpCode);
        if (!ok) return ResponseEntity.status(400).body(Map.of("error", "Unable to reset password"));
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken required"));
        }
        return authService.refresh(refreshToken)
                .map(result -> ResponseEntity.ok(Map.of(
                        "userId", result.userId(),
                        "accessToken", result.accessToken(),
                        "refreshToken", result.refreshToken(),
                        "tokenType", "Bearer",
                        "roles", result.roles()
                )))
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token")));
    }

    public record LoginRequest(String email, String password, String totpCode) {}
    public record RegisterRequest(String email, String password, String otpChannel, String phoneNumber) {}
    public record ForgotPasswordRequest(String email, String otpChannel, String phoneNumber) {}
}
