package com.eshop.user.controller;

import com.eshop.user.entity.UserProfile;
import com.eshop.user.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getProfile(@RequestHeader("X-User-Id") Long userId) {
        UserProfile profile = userProfileService.getOrCreate(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ProfileUpdateRequest request) {
        UserProfile updated = userProfileService.update(
                userId,
                request.fullName(),
                request.phone(),
                request.address()
        );
        return ResponseEntity.ok(updated);
    }

    public record ProfileUpdateRequest(String fullName, String phone, String address) {}
}
