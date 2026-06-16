package com.cristiane.salon.models.featureflag.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.featureflag.entity.FeatureFlag;
import com.cristiane.salon.models.featureflag.repository.FeatureFlagRepository;
import com.cristiane.salon.models.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @InjectMocks
    private FeatureFlagService featureFlagService;

    @Mock
    private FeatureFlagRepository repository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isEnabled_shouldReturnTrue_whenFlagExistsAndIsEnabled() {
        // Arrange
        String name = "my-flag";
        FeatureFlag flag = new FeatureFlag(name, true, "desc");
        when(repository.findById(name)).thenReturn(Optional.of(flag));

        // Act
        boolean result = featureFlagService.isEnabled(name);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isEnabled_shouldReturnFalse_whenFlagExistsAndIsDisabled() {
        // Arrange
        String name = "my-flag";
        FeatureFlag flag = new FeatureFlag(name, false, "desc");
        when(repository.findById(name)).thenReturn(Optional.of(flag));

        // Act
        boolean result = featureFlagService.isEnabled(name);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isEnabled_shouldReturnFalse_whenFlagDoesNotExist() {
        // Arrange
        String name = "non-existent";
        when(repository.findById(name)).thenReturn(Optional.empty());

        // Act
        boolean result = featureFlagService.isEnabled(name);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void findAll_shouldReturnAllFlags() {
        // Arrange
        List<FeatureFlag> flags = Collections.singletonList(new FeatureFlag("flag1", true, "desc1"));
        when(repository.findAll()).thenReturn(flags);

        // Act
        List<FeatureFlag> result = featureFlagService.findAll();

        // Assert
        assertThat(result).isSameAs(flags);
    }

    @Test
    void toggle_shouldThrowResourceNotFoundException_whenFlagDoesNotExist() {
        // Arrange
        String name = "non-existent";
        when(repository.findById(name)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureFlagService.toggle(name))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Feature Flag não encontrada: " + name);
    }

    @Test
    void toggle_shouldInvertEnabledStateAndLogSystemAudit_whenAuthIsNull() {
        // Arrange
        String name = "my-flag";
        FeatureFlag flag = new FeatureFlag(name, false, "desc");
        FeatureFlag savedFlag = new FeatureFlag(name, true, "desc");

        when(repository.findById(name)).thenReturn(Optional.of(flag));
        when(repository.save(flag)).thenReturn(savedFlag);

        // Act
        FeatureFlag result = featureFlagService.toggle(name);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnabled()).isTrue();
        verify(repository).save(flag);
        verify(auditLogService).logAction(
                eq(null),
                eq("SYSTEM"),
                eq("FEATURE_FLAG_TOGGLED"),
                eq("FeatureFlag"),
                eq(null),
                contains("\"from\":false,\"to\":true"),
                eq("SUCCESS")
        );
    }

    @Test
    void toggle_shouldInvertEnabledStateAndLogUserAudit_whenAuthIsUser() {
        // Arrange
        String name = "my-flag";
        FeatureFlag flag = new FeatureFlag(name, true, "desc");
        FeatureFlag savedFlag = new FeatureFlag(name, false, "desc");

        User user = new User();
        user.setId(10L);
        user.setEmail("admin@salon.com");

        when(repository.findById(name)).thenReturn(Optional.of(flag));
        when(repository.save(flag)).thenReturn(savedFlag);
        
        when(authentication.getName()).thenReturn("admin@salon.com");
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        FeatureFlag result = featureFlagService.toggle(name);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnabled()).isFalse();
        verify(repository).save(flag);
        verify(auditLogService).logAction(
                eq(10L),
                eq("admin@salon.com"),
                eq("FEATURE_FLAG_TOGGLED"),
                eq("FeatureFlag"),
                eq(null),
                contains("\"from\":true,\"to\":false"),
                eq("SUCCESS")
        );
    }

    @Test
    void toggle_shouldInvertEnabledStateAndLogStringAuthName_whenAuthPrincipalIsNotUser() {
        // Arrange
        String name = "my-flag";
        FeatureFlag flag = new FeatureFlag(name, true, "desc");
        FeatureFlag savedFlag = new FeatureFlag(name, false, "desc");

        when(repository.findById(name)).thenReturn(Optional.of(flag));
        when(repository.save(flag)).thenReturn(savedFlag);

        when(authentication.getName()).thenReturn("string_name");
        when(authentication.getPrincipal()).thenReturn("string_principal");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        FeatureFlag result = featureFlagService.toggle(name);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnabled()).isFalse();
        verify(repository).save(flag);
        verify(auditLogService).logAction(
                eq(null),
                eq("string_name"),
                eq("FEATURE_FLAG_TOGGLED"),
                eq("FeatureFlag"),
                eq(null),
                contains("\"from\":true,\"to\":false"),
                eq("SUCCESS")
        );
    }

    @Test
    void toggle_shouldCatchAuditExceptionSilently() {
        // Arrange
        String name = "my-flag";
        FeatureFlag flag = new FeatureFlag(name, false, "desc");
        FeatureFlag savedFlag = new FeatureFlag(name, true, "desc");

        when(repository.findById(name)).thenReturn(Optional.of(flag));
        when(repository.save(flag)).thenReturn(savedFlag);

        // Force exception
        doThrow(new RuntimeException("Audit Error")).when(auditLogService).logAction(
                any(), any(), any(), any(), any(), any(), any()
        );

        // Act
        FeatureFlag result = featureFlagService.toggle(name);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnabled()).isTrue();
        verify(repository).save(flag);
        verify(auditLogService).logAction(any(), any(), any(), any(), any(), any(), any());
    }
}
