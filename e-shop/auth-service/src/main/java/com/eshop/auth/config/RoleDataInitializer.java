package com.eshop.auth.config;

import com.eshop.auth.entity.Role;
import com.eshop.auth.repository.RoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleDataInitializer {

    @Bean
    public ApplicationRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.count() == 0) {
                Role customer = new Role();
                customer.setRoleName("customer");
                roleRepository.save(customer);
                Role admin = new Role();
                admin.setRoleName("admin");
                roleRepository.save(admin);
                Role merchant = new Role();
                merchant.setRoleName("merchant");
                roleRepository.save(merchant);
            }
        };
    }
}
