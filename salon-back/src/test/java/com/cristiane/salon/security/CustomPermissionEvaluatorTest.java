package com.cristiane.salon.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomPermissionEvaluatorTest {

    @InjectMocks
    private CustomPermissionEvaluator permissionEvaluator;

    @Mock
    private Authentication authentication;

    private List<GrantedAuthority> authorities;

    @BeforeEach
    void setUp() {
        authorities = new ArrayList<>();
    }

    @Test
    void hasPermission_shouldReturnFalse_whenAuthoritiesListIsEmpty() {
        // Arrange
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "GET");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_shouldIgnoreRoleAuthorities() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "GET");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_shouldIgnoreInvalidAuthorityFormat() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("INVALID_FORMAT_WITHOUT_COLON"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "GET");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_shouldReturnFalse_whenMethodMatchesButEndpointDoesNot() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("GET:/v1/products"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "GET");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_shouldReturnFalse_whenEndpointMatchesButMethodDoesNot() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("POST:/v1/users"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "GET");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_shouldReturnTrue_whenMethodIsWildcardAndEndpointMatches() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("*:/v1/users"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "POST");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_shouldReturnTrue_whenMethodAndEndpointMatchesExact() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("GET:/v1/users"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users", "GET");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_shouldReturnTrue_whenMethodAndEndpointMatchesCaseInsensitiveAndAntPattern() {
        // Arrange
        authorities.add(new SimpleGrantedAuthority("get:/v1/users/**"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        // Act
        boolean result = permissionEvaluator.hasPermission(authentication, "/v1/users/123/profile", "GET");

        // Assert
        assertThat(result).isTrue();
    }
}
