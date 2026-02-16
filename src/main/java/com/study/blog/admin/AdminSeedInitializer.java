package com.study.blog.admin;

import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class AdminSeedInitializer implements ApplicationRunner {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final boolean seedEnabled;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminName;
    private final String disallowedPassword;
    private final boolean adminMustChangePassword;

    public AdminSeedInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                Environment environment,
                                @Value("${app.admin.seed.enabled:false}") boolean seedEnabled,
                                @Value("${app.admin.seed.username:}") String adminUsername,
                                @Value("${app.admin.seed.password:}") String adminPassword,
                                @Value("${app.admin.seed.name:}") String adminName,
                                @Value("${app.admin.seed.disallowed-password:}") String disallowedPassword,
                                @Value("${app.admin.seed.must-change-password:true}") boolean adminMustChangePassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.seedEnabled = seedEnabled;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
        this.disallowedPassword = disallowedPassword;
        this.adminMustChangePassword = adminMustChangePassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        String normalizedPassword = normalizePassword(adminPassword);
        validateSeedConfigOrFailFast(normalizedPassword);
        validateProdSeedPasswordOrFailFast(normalizedPassword);

        String normalizedUsername = UserNamePolicy.normalizeUsername(adminUsername);
        seedAdminWithRetry(normalizedUsername, normalizedPassword);
    }

    private void seedAdminWithRetry(String normalizedUsername, String normalizedPassword) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                upsertAdmin(normalizedUsername, normalizedPassword);
                return;
            } catch (PessimisticLockingFailureException ex) {
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    // Seed failure should not block application startup.
                    log.error("Admin seed skipped due to lock timeout after {} attempts. username={}",
                            MAX_RETRY_ATTEMPTS, normalizedUsername, ex);
                    return;
                }

                long backoffMillis = 250L * attempt;
                log.warn("Admin seed lock timeout (attempt {}/{}). Retrying in {}ms. username={}",
                        attempt, MAX_RETRY_ATTEMPTS, backoffMillis, normalizedUsername);
                sleepQuietly(backoffMillis);
            }
        }
    }

    private void upsertAdmin(String normalizedUsername, String normalizedPassword) {
        userRepository.findByUsername(normalizedUsername).ifPresentOrElse(existing -> {
            boolean changed = false;
            if (!"N".equalsIgnoreCase(existing.getDeletedYn())) {
                existing.setDeletedYn("N");
                changed = true;
            }
            if (existing.getRole() != UserRole.ADMIN) {
                existing.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (existing.getStatus() != UserStatus.ACTIVE) {
                existing.setStatus(UserStatus.ACTIVE);
                changed = true;
            }
            if (!Objects.equals(existing.getMustChangePassword(), adminMustChangePassword)) {
                existing.setMustChangePassword(adminMustChangePassword);
                changed = true;
            }
            String desiredName = UserNamePolicy.resolveName(adminName, normalizedUsername);
            if (!desiredName.equals(existing.getName())) {
                existing.setName(desiredName);
                changed = true;
            }
            if (changed) {
                userRepository.save(existing);
                log.info("Admin seed existing account updated: username={}", normalizedUsername);
            } else {
                log.info("Admin seed existing account kept: username={}", normalizedUsername);
            }
        }, () -> {
            User admin = User.builder()
                    .username(normalizedUsername)
                    .password(passwordEncoder.encode(normalizedPassword))
                    .name(UserNamePolicy.resolveName(adminName, normalizedUsername))
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .mustChangePassword(adminMustChangePassword)
                    .deletedYn("N")
                    .build();
            userRepository.save(admin);
            log.info("Admin seed account created: username={}", normalizedUsername);
        });
    }

    private void validateSeedConfigOrFailFast(String normalizedPassword) {
        String normalizedUsername = UserNamePolicy.normalizeUsername(adminUsername);
        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            throw new IllegalStateException("APP_ADMIN_SEED_ENABLED=true requires APP_ADMIN_USERNAME.");
        }
        if (normalizedPassword.isEmpty()) {
            throw new IllegalStateException("APP_ADMIN_SEED_ENABLED=true requires APP_ADMIN_PASSWORD.");
        }
    }

    private void validateProdSeedPasswordOrFailFast(String normalizedPassword) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        String normalizedDisallowedPassword = normalizePassword(disallowedPassword);
        if (!normalizedDisallowedPassword.isEmpty() && normalizedDisallowedPassword.equals(normalizedPassword)) {
            throw new IllegalStateException(
                    "In prod profile, APP_ADMIN_PASSWORD cannot use APP_ADMIN_SEED_DISALLOWED_PASSWORD.");
        }
    }

    private String normalizePassword(String password) {
        return password == null ? "" : password.trim();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
