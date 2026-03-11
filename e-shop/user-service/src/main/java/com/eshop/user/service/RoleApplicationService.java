package com.eshop.user.service;

import com.eshop.user.entity.AuthRole;
import com.eshop.user.entity.AuthUserRole;
import com.eshop.user.entity.RoleApplication;
import com.eshop.user.repository.AuthRoleRepository;
import com.eshop.user.repository.AuthUserRoleRepository;
import com.eshop.user.repository.RoleApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RoleApplicationService {

    private final RoleApplicationRepository roleApplicationRepository;
    private final AuthRoleRepository authRoleRepository;
    private final AuthUserRoleRepository authUserRoleRepository;

    public RoleApplicationService(RoleApplicationRepository roleApplicationRepository,
                                  AuthRoleRepository authRoleRepository,
                                  AuthUserRoleRepository authUserRoleRepository) {
        this.roleApplicationRepository = roleApplicationRepository;
        this.authRoleRepository = authRoleRepository;
        this.authUserRoleRepository = authUserRoleRepository;
    }

    public List<RoleApplication> listMine(Long userId) {
        return roleApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<RoleApplication> listAll() {
        return roleApplicationRepository.findAll();
    }

    @Transactional
    public RoleApplication apply(Long userId,
                                 RoleApplication.RequestedRole requestedRole,
                                 String fullName,
                                 String email,
                                 String phone,
                                 String governmentId,
                                 String drivingLicense,
                                 String shopName,
                                 Integer yearsExperience) {
        RoleApplication app = new RoleApplication();
        app.setUserId(userId);
        app.setRequestedRole(requestedRole);
        app.setFullName(trim(fullName));
        app.setEmail(trim(email));
        app.setPhone(trim(phone));
        app.setGovernmentId(trim(governmentId));
        app.setDrivingLicense(trim(drivingLicense));
        app.setShopName(trim(shopName));
        app.setYearsExperience(yearsExperience == null ? 0 : yearsExperience);
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());

        String summary = evaluateAndSummarize(app);
        app.setCredentialsSummary(summary);

        if (isEligible(app)) {
            app.setStatus(RoleApplication.ApplicationStatus.APPROVED);
            app.setReviewNotes("Auto-approved based on credential checks.");
            roleApplicationRepository.save(app);
            grantRole(userId, requestedRole);
        } else {
            app.setStatus(RoleApplication.ApplicationStatus.UNDER_REVIEW);
            app.setReviewNotes("Needs manual review: credentials did not pass auto-check.");
            roleApplicationRepository.save(app);
        }
        return app;
    }

    @Transactional
    public Optional<RoleApplication> review(Long id, String action, String notes) {
        return roleApplicationRepository.findById(id).map(app -> {
            String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
            if ("APPROVE".equals(normalized)) {
                app.setStatus(RoleApplication.ApplicationStatus.APPROVED);
                grantRole(app.getUserId(), app.getRequestedRole());
            } else if ("REJECT".equals(normalized)) {
                app.setStatus(RoleApplication.ApplicationStatus.REJECTED);
            } else if ("REVIEW".equals(normalized)) {
                app.setStatus(RoleApplication.ApplicationStatus.UNDER_REVIEW);
            }
            if (notes != null && !notes.isBlank()) {
                app.setReviewNotes(notes.trim());
            }
            app.setUpdatedAt(Instant.now());
            return roleApplicationRepository.save(app);
        });
    }

    private String evaluateAndSummarize(RoleApplication app) {
        boolean governmentIdOk = app.getGovernmentId() != null && app.getGovernmentId().matches("^[A-Za-z0-9]{8,20}$");
        boolean phoneOk = app.getPhone() != null && app.getPhone().replaceAll("\\D", "").matches("^(91)?[6-9]\\d{9}$");
        int years = app.getYearsExperience() == null ? 0 : app.getYearsExperience();
        if (app.getRequestedRole() == RoleApplication.RequestedRole.MERCHANT) {
            boolean shopOk = app.getShopName() != null && app.getShopName().length() >= 3;
            boolean expOk = years >= 1;
            return "merchant-check: govId=" + governmentIdOk + ", phone=" + phoneOk + ", shop=" + shopOk + ", exp=" + expOk;
        }
        boolean dlOk = app.getDrivingLicense() != null && app.getDrivingLicense().matches("^[A-Za-z0-9-]{8,20}$");
        boolean expOk = years >= 0;
        return "delivery-check: govId=" + governmentIdOk + ", phone=" + phoneOk + ", drivingLicense=" + dlOk + ", exp=" + expOk;
    }

    private boolean isEligible(RoleApplication app) {
        boolean governmentIdOk = app.getGovernmentId() != null && app.getGovernmentId().matches("^[A-Za-z0-9]{8,20}$");
        boolean phoneOk = app.getPhone() != null && app.getPhone().replaceAll("\\D", "").matches("^(91)?[6-9]\\d{9}$");
        int years = app.getYearsExperience() == null ? 0 : app.getYearsExperience();
        if (app.getRequestedRole() == RoleApplication.RequestedRole.MERCHANT) {
            boolean shopOk = app.getShopName() != null && app.getShopName().length() >= 3;
            return governmentIdOk && phoneOk && shopOk && years >= 1;
        }
        boolean dlOk = app.getDrivingLicense() != null && app.getDrivingLicense().matches("^[A-Za-z0-9-]{8,20}$");
        return governmentIdOk && phoneOk && dlOk;
    }

    private void grantRole(Long userId, RoleApplication.RequestedRole requestedRole) {
        String roleName = requestedRole == RoleApplication.RequestedRole.MERCHANT ? "merchant" : "delivery_agent";
        AuthRole role = authRoleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    AuthRole r = new AuthRole();
                    r.setRoleName(roleName);
                    return authRoleRepository.save(r);
                });
        if (!authUserRoleRepository.existsByUserIdAndRoleId(userId, role.getId())) {
            authUserRoleRepository.save(new AuthUserRole(userId, role.getId()));
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
