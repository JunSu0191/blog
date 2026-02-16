package com.study.blog.admin;

import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeedInitializerTest {
    private static final String SEED_PASSWORD = "seed-password";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Environment environment;
    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void shouldCreateAdminWhenEnabledAndAccountMissing() throws Exception {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(SEED_PASSWORD)).thenReturn("encoded");

        AdminSeedInitializer initializer = new AdminSeedInitializer(
                userRepository,
                passwordEncoder,
                environment,
                true,
                "admin",
                SEED_PASSWORD,
                "admin name",
                "",
                true);

        initializer.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getMustChangePassword()).isTrue();
    }

    @Test
    void shouldBeIdempotentWhenAdminAlreadyExists() throws Exception {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        User existing = User.builder()
                .id(1L)
                .username("admin")
                .name("admin name")
                .password("encoded")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .deletedYn("N")
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        AdminSeedInitializer initializer = new AdminSeedInitializer(
                userRepository,
                passwordEncoder,
                environment,
                true,
                "admin",
                SEED_PASSWORD,
                "admin name",
                "",
                true);

        initializer.run(applicationArguments);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFailFastWhenProdAndDisallowedPassword() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        AdminSeedInitializer initializer = new AdminSeedInitializer(
                userRepository,
                passwordEncoder,
                environment,
                true,
                "admin",
                SEED_PASSWORD,
                "admin name",
                SEED_PASSWORD,
                true);

        assertThatThrownBy(() -> initializer.run(applicationArguments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ADMIN_SEED_DISALLOWED_PASSWORD");
    }

    @Test
    void shouldUpdateExistingAdminMustChangePasswordWhenConfiguredFalse() throws Exception {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        User existing = User.builder()
                .id(1L)
                .username("admin")
                .name("admin name")
                .password("encoded")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .deletedYn("N")
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        AdminSeedInitializer initializer = new AdminSeedInitializer(
                userRepository,
                passwordEncoder,
                environment,
                true,
                "admin",
                SEED_PASSWORD,
                "admin name",
                "",
                false);

        initializer.run(applicationArguments);

        assertThat(existing.getMustChangePassword()).isFalse();
        verify(userRepository).save(existing);
    }
}
