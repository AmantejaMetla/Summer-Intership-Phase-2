package com.eshop.user.repository;

import com.eshop.user.entity.RoleApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleApplicationRepository extends JpaRepository<RoleApplication, Long> {
    List<RoleApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
}
