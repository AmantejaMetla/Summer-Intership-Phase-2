package com.eshop.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_credentials", indexes = @Index(unique = true, columnList = "email"))
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 16)
    private String otpDeliveryPreference = "EMAIL";

    private String emailVerificationOtp;
    private Instant emailVerificationOtpExpiry;

    @Column(length = 128)
    private String totpSecret;

    @Column(nullable = false)
    private boolean totpEnabled = false;

    private String passwordResetOtp;
    private Instant passwordResetOtpExpiry;

    @Column(length = 128)
    private String passwordResetToken;
    private Instant passwordResetTokenExpiry;

    public UserCredential() {
    }

    public UserCredential(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmailVerificationOtp() {
        return emailVerificationOtp;
    }

    public void setEmailVerificationOtp(String emailVerificationOtp) {
        this.emailVerificationOtp = emailVerificationOtp;
    }

    public Instant getEmailVerificationOtpExpiry() {
        return emailVerificationOtpExpiry;
    }

    public void setEmailVerificationOtpExpiry(Instant emailVerificationOtpExpiry) {
        this.emailVerificationOtpExpiry = emailVerificationOtpExpiry;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtpDeliveryPreference() {
        return otpDeliveryPreference;
    }

    public void setOtpDeliveryPreference(String otpDeliveryPreference) {
        this.otpDeliveryPreference = otpDeliveryPreference;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public String getPasswordResetOtp() {
        return passwordResetOtp;
    }

    public void setPasswordResetOtp(String passwordResetOtp) {
        this.passwordResetOtp = passwordResetOtp;
    }

    public Instant getPasswordResetOtpExpiry() {
        return passwordResetOtpExpiry;
    }

    public void setPasswordResetOtpExpiry(Instant passwordResetOtpExpiry) {
        this.passwordResetOtpExpiry = passwordResetOtpExpiry;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public Instant getPasswordResetTokenExpiry() {
        return passwordResetTokenExpiry;
    }

    public void setPasswordResetTokenExpiry(Instant passwordResetTokenExpiry) {
        this.passwordResetTokenExpiry = passwordResetTokenExpiry;
    }
}
