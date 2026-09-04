package com.employee.employeeingestionplatform.config;

import com.employee.employeeingestionplatform.entity.ApplicationUser;
import com.employee.employeeingestionplatform.entity.UserRole;
import com.employee.employeeingestionplatform.repository.ApplicationUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            ApplicationUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin.username}") String adminUsername,
            @Value("${app.security.admin.password}") String adminPassword,
            @Value("${app.security.user.username}") String userUsername,
            @Value("${app.security.user.password}") String userPassword
    ) {
        return args -> {
            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    adminUsername,
                    adminPassword,
                    UserRole.ADMIN
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    userUsername,
                    userPassword,
                    UserRole.USER
            );
        };
    }

    private void createUserIfMissing(
            ApplicationUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword,
            UserRole role
    ) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        ApplicationUser user = new ApplicationUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);

        userRepository.save(user);
    }
}