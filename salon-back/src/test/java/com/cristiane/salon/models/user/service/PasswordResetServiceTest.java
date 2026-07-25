package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.integrations.email.service.EmailService;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.user.entity.PasswordResetToken;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.PasswordResetTokenRepository;
import com.cristiane.salon.models.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository, tokenRepository, passwordEncoder, emailService, auditLogService
        );

        user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");
        user.setName("Cliente");
        user.setPassword("old-hashed-password");
    }

    // --- requestReset ---

    @Test
    void requestReset_whenEmailDoesNotExist_shouldDoNothingSilently() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@example.com");

        verifyNoInteractions(emailService, auditLogService, tokenRepository);
    }

    @Test
    void requestReset_whenEmailExists_shouldInvalidateOldTokensAndSendEmail() {
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestReset("client@example.com");

        verify(tokenRepository).deleteByUserId(1L);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedToken.isValid()).isTrue();

        verify(emailService).sendPasswordResetEmail(eq(user), anyString());
        verify(auditLogService).logAction(
                eq(1L), eq("client@example.com"), eq("PASSWORD_RESET_REQUESTED"),
                eq("User"), eq(1L), anyString(), eq("SUCCESS")
        );
    }

    // --- resetPassword ---

    @Test
    void resetPassword_whenTokenNotFound_shouldThrowBadRequestException() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bogus-token", "NewPass123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Link de redefinição inválido ou expirado.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_whenTokenExpired_shouldThrowBadRequestException() {
        PasswordResetToken expired = PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash")
                .createdAt(LocalDateTime.now().minusHours(2))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "NewPass123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Link de redefinição inválido ou expirado.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_whenTokenAlreadyUsed_shouldThrowBadRequestException() {
        PasswordResetToken used = PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "NewPass123"))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_whenTokenValid_shouldUpdatePasswordAndMarkTokenUsed() {
        PasswordResetToken valid = PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(25))
                .build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(valid));
        when(passwordEncoder.encode("NewPass123")).thenReturn("new-hashed-password");

        passwordResetService.resetPassword("raw-token", "NewPass123");

        assertThat(user.getPassword()).isEqualTo("new-hashed-password");
        verify(userRepository).save(user);

        assertThat(valid.getUsedAt()).isNotNull();
        verify(tokenRepository, times(1)).save(valid);

        verify(auditLogService).logAction(
                eq(1L), eq("client@example.com"), eq("PASSWORD_RESET_COMPLETED"),
                eq("User"), eq(1L), anyString(), eq("SUCCESS")
        );
    }
}
