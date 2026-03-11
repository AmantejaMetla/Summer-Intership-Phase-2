package com.eshop.auth.service;

import com.eshop.auth.entity.RefreshToken;
import com.eshop.auth.entity.Role;
import com.eshop.auth.entity.UserCredential;
import com.eshop.auth.entity.UserRole;
import com.eshop.auth.repository.RefreshTokenRepository;
import com.eshop.auth.repository.RoleRepository;
import com.eshop.auth.repository.UserCredentialRepository;
import com.eshop.auth.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserCredentialRepository userCredentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final OtpDispatchService otpDispatchService;
    private final TotpService totpService;

    @Value("${jwt.refresh-token-validity-days:7}")
    private int refreshTokenValidityDays;
    @Value("${app.otp-expiry-minutes:10}")
    private int otpExpiryMinutes;
    @Value("${app.reset-token-expiry-minutes:15}")
    private int resetTokenExpiryMinutes;

    public AuthService(UserCredentialRepository userCredentialRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       OtpDispatchService otpDispatchService,
                       TotpService totpService) {
        this.userCredentialRepository = userCredentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.otpDispatchService = otpDispatchService;
        this.totpService = totpService;
    }

    public List<String> getRolesForUser(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getRoleName())
                .collect(Collectors.toList());
    }

    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Optional<Role> assignRoleToUser(Long userId, Long roleId) {
        if (!userCredentialRepository.existsById(userId)) {
            return Optional.empty();
        }
        if (userRoleRepository.findByUserId(userId).stream()
                .anyMatch(ur -> ur.getRoleId().equals(roleId))) {
            return roleRepository.findById(roleId);
        }
        return roleRepository.findById(roleId)
                .map(role -> {
                    userRoleRepository.save(new UserRole(userId, roleId));
                    return role;
                });
    }

    @Transactional
    public Optional<LoginResult> login(String email, String password, String totpCode) {
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .filter(uc -> passwordEncoder.matches(password, uc.getPasswordHash()))
                .filter(UserCredential::isEmailVerified)
                .filter(UserCredential::isTotpEnabled)
                .filter(uc -> totpService.verifyCode(uc.getTotpSecret(), totpCode))
                .map(uc -> {
                    List<String> roles = getRolesForUser(uc.getId());
                    String accessToken = jwtService.generateAccessToken(uc.getId(), roles);
                    String refreshToken = jwtService.generateRefreshToken();
                    refreshTokenRepository.deleteByUserId(uc.getId());
                    refreshTokenRepository.save(new RefreshToken(
                            refreshToken,
                            uc.getId(),
                            Instant.now().plusSeconds(refreshTokenValidityDays * 86400L)
                    ));
                    return new LoginResult(uc.getId(), accessToken, refreshToken, roles);
                });
    }

    @Transactional
    public Optional<RegisterResult> registerInit(String email, String password, String otpChannelRaw, String phoneNumberRaw) {
        String normalized = normalizeEmail(email);
        if (!isLikelyRealEmail(normalized) || password == null || password.length() < 8) {
            return Optional.empty();
        }
        OtpDeliveryChannel channel = parseChannel(otpChannelRaw);
        String normalizedPhone = normalizeIndianPhone(phoneNumberRaw);
        if (channel == OtpDeliveryChannel.SMS && normalizedPhone == null) {
            return Optional.empty();
        }
        Optional<UserCredential> existing = userCredentialRepository.findByEmail(normalized);
        UserCredential uc;
        boolean isNewUser = existing.isEmpty();
        if (existing.isPresent()) {
            uc = existing.get();
            // Allow re-register only for unverified accounts by resending OTP.
            if (uc.isEmailVerified()) {
                return Optional.empty();
            }
            uc.setPasswordHash(passwordEncoder.encode(password));
        } else {
            uc = new UserCredential(normalized, passwordEncoder.encode(password));
        }
        String otp = otpService.generateNumericOtp(6);
        uc.setEmailVerificationOtp(otp);
        uc.setEmailVerificationOtpExpiry(Instant.now().plusSeconds(otpExpiryMinutes * 60L));
        uc.setPhoneNumber(normalizedPhone);
        uc.setOtpDeliveryPreference(channel.name());
        uc = userCredentialRepository.save(uc);
        final long savedUserId = uc.getId();
        if (isNewUser) {
            roleRepository.findByRoleName("customer").ifPresent(role ->
                    userRoleRepository.save(new UserRole(savedUserId, role.getId())));
        }
        otpDispatchService.sendOtp(
                channel,
                normalized,
                normalizedPhone,
                "E-Shop Email Verification OTP",
                "Your OTP is: " + otp + ". It expires in " + otpExpiryMinutes + " minutes."
        );
        return Optional.of(new RegisterResult(uc.getId(), normalized, "VERIFY_EMAIL"));
    }

    @Transactional
    public boolean verifyEmailOtp(String email, String otp) {
        String normalized = normalizeEmail(email);
        return userCredentialRepository.findByEmail(normalized)
                .filter(uc -> uc.getEmailVerificationOtp() != null)
                .filter(uc -> uc.getEmailVerificationOtpExpiry() != null && uc.getEmailVerificationOtpExpiry().isAfter(Instant.now()))
                .filter(uc -> uc.getEmailVerificationOtp().equals(otp))
                .map(uc -> {
                    uc.setEmailVerified(true);
                    uc.setEmailVerificationOtp(null);
                    uc.setEmailVerificationOtpExpiry(null);
                    userCredentialRepository.save(uc);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public Optional<TotpSetupResult> setupTotp(String email, String password) {
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .filter(UserCredential::isEmailVerified)
                .filter(uc -> passwordEncoder.matches(password, uc.getPasswordHash()))
                .map(uc -> {
                    String secret = totpService.generateSecretBase32();
                    uc.setTotpSecret(secret);
                    uc.setTotpEnabled(false);
                    userCredentialRepository.save(uc);
                    String uri = totpService.buildOtpAuthUri("E-Shop", uc.getEmail(), secret);
                    return new TotpSetupResult(uc.getId(), secret, uri);
                });
    }

    @Transactional
    public boolean confirmTotp(String email, String otpCode) {
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .filter(uc -> uc.getTotpSecret() != null && !uc.getTotpSecret().isBlank())
                .filter(uc -> totpService.verifyCode(uc.getTotpSecret(), otpCode))
                .map(uc -> {
                    uc.setTotpEnabled(true);
                    userCredentialRepository.save(uc);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public boolean requestForgotPasswordOtp(String email, String otpChannelRaw, String phoneNumberRaw) {
        String normalized = normalizeEmail(email);
        return userCredentialRepository.findByEmail(normalized).map(uc -> {
            OtpDeliveryChannel preferred = parseChannel(otpChannelRaw);
            String selectedPhone = uc.getPhoneNumber();
            if (preferred == OtpDeliveryChannel.SMS) {
                String normalizedPhone = normalizeIndianPhone(phoneNumberRaw);
                if (normalizedPhone == null || selectedPhone == null || !selectedPhone.equals(normalizedPhone)) {
                    return false;
                }
            }
            String otp = otpService.generateNumericOtp(6);
            uc.setPasswordResetOtp(otp);
            uc.setPasswordResetOtpExpiry(Instant.now().plusSeconds(otpExpiryMinutes * 60L));
            uc.setPasswordResetToken(null);
            uc.setPasswordResetTokenExpiry(null);
            userCredentialRepository.save(uc);
            otpDispatchService.sendOtp(
                    preferred,
                    normalized,
                    selectedPhone,
                    "E-Shop Password Reset OTP",
                    "Your password reset OTP is: " + otp + ". It expires in " + otpExpiryMinutes + " minutes."
            );
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<String> verifyForgotPasswordOtp(String email, String otp) {
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .filter(uc -> uc.getPasswordResetOtp() != null)
                .filter(uc -> uc.getPasswordResetOtpExpiry() != null && uc.getPasswordResetOtpExpiry().isAfter(Instant.now()))
                .filter(uc -> uc.getPasswordResetOtp().equals(otp))
                .map(uc -> {
                    String resetToken = UUID.randomUUID().toString();
                    uc.setPasswordResetToken(resetToken);
                    uc.setPasswordResetTokenExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60L));
                    uc.setPasswordResetOtp(null);
                    uc.setPasswordResetOtpExpiry(null);
                    userCredentialRepository.save(uc);
                    return resetToken;
                });
    }

    @Transactional
    public boolean resetPasswordWith2fa(String email, String resetToken, String newPassword, String totpCode) {
        if (newPassword == null || newPassword.length() < 8) return false;
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .filter(uc -> uc.getPasswordResetToken() != null && uc.getPasswordResetToken().equals(resetToken))
                .filter(uc -> uc.getPasswordResetTokenExpiry() != null && uc.getPasswordResetTokenExpiry().isAfter(Instant.now()))
                .filter(UserCredential::isTotpEnabled)
                .filter(uc -> totpService.verifyCode(uc.getTotpSecret(), totpCode))
                .map(uc -> {
                    uc.setPasswordHash(passwordEncoder.encode(newPassword));
                    uc.setPasswordResetToken(null);
                    uc.setPasswordResetTokenExpiry(null);
                    userCredentialRepository.save(uc);
                    refreshTokenRepository.deleteByUserId(uc.getId());
                    return true;
                }).orElse(false);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private OtpDeliveryChannel parseChannel(String value) {
        if (value == null || value.isBlank()) {
            return OtpDeliveryChannel.EMAIL;
        }
        try {
            return OtpDeliveryChannel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return OtpDeliveryChannel.EMAIL;
        }
    }

    private String normalizeIndianPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (digits.length() != 10 || !digits.matches("[6-9][0-9]{9}")) {
            return null;
        }
        return "+91" + digits;
    }

    private boolean isLikelyRealEmail(String email) {
        if (email == null) return false;
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return false;
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        return !domain.endsWith(".local") && !domain.equals("example.com") && !domain.equals("test.com");
    }

    public Optional<LoginResult> refresh(String refreshTokenValue) {
        return refreshTokenRepository.findByToken(refreshTokenValue)
                .filter(rt -> rt.getExpiryDate().isAfter(Instant.now()))
                .map(rt -> {
                    refreshTokenRepository.delete(rt);
                    List<String> roles = getRolesForUser(rt.getUserId());
                    String newAccess = jwtService.generateAccessToken(rt.getUserId(), roles);
                    String newRefresh = jwtService.generateRefreshToken();
                    refreshTokenRepository.save(new RefreshToken(
                            newRefresh,
                            rt.getUserId(),
                            Instant.now().plusSeconds(refreshTokenValidityDays * 86400L)
                    ));
                    return new LoginResult(rt.getUserId(), newAccess, newRefresh, roles);
                });
    }

    public record RegisterResult(Long userId, String email, String nextStep) {}
    public record TotpSetupResult(Long userId, String secret, String otpAuthUri) {}
    public record LoginResult(Long userId, String accessToken, String refreshToken, List<String> roles) {}
}
