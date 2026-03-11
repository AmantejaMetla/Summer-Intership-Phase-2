package com.eshop.user.service;

import com.eshop.user.entity.UserProfile;
import com.eshop.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<UserProfile> getByAuthId(Long authId) {
        return userProfileRepository.findByAuthId(authId);
    }

    public UserProfile getOrCreate(Long authId) {
        return userProfileRepository.findByAuthId(authId)
                .orElseGet(() -> userProfileRepository.save(new UserProfile(authId)));
    }

    public UserProfile update(Long authId, String fullName, String phone, String address) {
        UserProfile profile = getOrCreate(authId);
        if (fullName != null) profile.setFullName(fullName);
        if (phone != null) profile.setPhone(phone);
        if (address != null) profile.setAddress(address);
        return userProfileRepository.save(profile);
    }
}
