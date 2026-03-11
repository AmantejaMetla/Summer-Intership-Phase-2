package com.eshop.user.repository;

import com.eshop.user.entity.AuthUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRoleRepository extends JpaRepository<AuthUserRole, Long> {
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
