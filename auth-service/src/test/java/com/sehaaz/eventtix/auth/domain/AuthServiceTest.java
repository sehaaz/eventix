package com.sehaaz.eventtix.auth.domain;

import com.sehaaz.eventtix.auth.api.dto.LoginRequest;
import com.sehaaz.eventtix.auth.api.dto.LoginResponse;
import com.sehaaz.eventtix.auth.api.dto.RegisterRequest;
import com.sehaaz.eventtix.auth.api.dto.UserResponse;
import com.sehaaz.eventtix.auth.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtIssuer jwtIssuer;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtIssuer, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void register_savesUserWithHashedPasswordAndUserRole() {
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = authService.register(new RegisterRequest(" Ali@Test.com ", "secret123", "Ali Veli"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response).isEqualTo(new UserResponse(1L, "ali@test.com", "Ali Veli", "USER"));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ali@test.com", "secret123", "Ali")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getError()).isEqualTo("EMAIL_EXISTS");
                });
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = user();
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtIssuer.issue(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("ali@test.com", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertInvalidCredentials(new LoginRequest("ali@test.com", "wrong"));
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("yok@test.com")).thenReturn(Optional.empty());

        assertInvalidCredentials(new LoginRequest("yok@test.com", "secret123"));
    }

    private void assertInvalidCredentials(LoginRequest request) {
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getError()).isEqualTo("INVALID_CREDENTIALS");
                });
        verify(jwtIssuer, never()).issue(any());
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("ali@test.com");
        user.setPasswordHash("hashed");
        user.setFullName("Ali");
        user.setRole(Role.USER);
        return user;
    }
}
