package com.eshop.user.controller;

import com.eshop.user.entity.RoleApplication;
import com.eshop.user.service.RoleApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/role-applications")
public class RoleApplicationController {

    private final RoleApplicationService roleApplicationService;

    public RoleApplicationController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    @PostMapping
    public ResponseEntity<?> apply(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ApplyRoleRequest request) {
        if (request == null || request.requestedRole() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "requestedRole is required"));
        }
        RoleApplication app = roleApplicationService.apply(
                userId,
                request.requestedRole(),
                request.fullName(),
                request.email(),
                request.phone(),
                request.governmentId(),
                request.drivingLicense(),
                request.shopName(),
                request.yearsExperience()
        );
        return ResponseEntity.ok(app);
    }

    @GetMapping("/me")
    public List<RoleApplication> myApplications(@RequestHeader("X-User-Id") Long userId) {
        return roleApplicationService.listMine(userId);
    }

    @GetMapping("/admin")
    public ResponseEntity<?> listAll(@RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        if (!hasRole(rolesHeader, "admin")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return ResponseEntity.ok(roleApplicationService.listAll());
    }

    @PatchMapping("/admin/{id}")
    public ResponseEntity<?> review(
            @PathVariable Long id,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader,
            @RequestBody Map<String, String> body) {
        if (!hasRole(rolesHeader, "admin")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        String action = body == null ? null : body.get("action");
        String notes = body == null ? null : body.get("notes");
        return roleApplicationService.review(id, action, notes)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Application not found")));
    }

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> role.equalsIgnoreCase(r));
    }

    public record ApplyRoleRequest(
            RoleApplication.RequestedRole requestedRole,
            String fullName,
            String email,
            String phone,
            String governmentId,
            String drivingLicense,
            String shopName,
            Integer yearsExperience
    ) {}
}
