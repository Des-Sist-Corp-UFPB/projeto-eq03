package com.cristiane.salon.security;

import com.cristiane.salon.models.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyUserPermissionsTest {

    @InjectMocks
    private VerifyUserPermissions verifyUserPermissions;

    @Mock
    private CustomPermissionEvaluator permissionEvaluator;

    @Mock
    private HttpServletRequest request;

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
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenAuthenticationIsNull() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(null);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenAuthenticationIsNotAuthenticated() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenPrincipalIsAnonymousUser() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenPrincipalIsNotCustomUser() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("genericUser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnTrue_whenUserIsSysadmin() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSADMIN")))
                .when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenUserIsAdminAndUriIsSysadmin() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/sysadmin/feature-flags");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenUserIsAdminAndUriIsAudit() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/audit/123");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnTrue_whenUserIsAdminAndUriIsGeneric() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/products");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnTrue_whenUserHasExplicitPermission() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/products");
        when(request.getMethod()).thenReturn("POST");
        when(permissionEvaluator.hasPermission(authentication, "/v1/products", "POST")).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenNoPermissionAndMethodIsDelete() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/products/1");
        when(request.getMethod()).thenReturn("DELETE");
        when(permissionEvaluator.hasPermission(authentication, "/v1/products/1", "DELETE")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(10L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenNoPermissionAndMethodIsPost() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/products");
        when(request.getMethod()).thenReturn("POST");
        when(permissionEvaluator.hasPermission(authentication, "/v1/products", "POST")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(10L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenNoPermissionAndMethodIsGetAndUserIdIsNull() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/products");
        when(request.getMethod()).thenReturn("GET");
        when(permissionEvaluator.hasPermission(authentication, "/v1/products", "GET")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnTrue_whenNoPermissionAndMethodIsGetAndUserIdMatches() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/users/10");
        when(request.getMethod()).thenReturn("GET");
        when(permissionEvaluator.hasPermission(authentication, "/v1/users/10", "GET")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(10L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void userOwnResourceOrHasPermission_shouldReturnFalse_whenNoPermissionAndMethodIsGetAndUserIdMismatches() {
        // Arrange
        User user = new User();
        user.setId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(request.getRequestURI()).thenReturn("/v1/users/20");
        when(request.getMethod()).thenReturn("GET");
        when(permissionEvaluator.hasPermission(authentication, "/v1/users/20", "GET")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        boolean result = verifyUserPermissions.userOwnResourceOrHasPermission(20L);

        // Assert
        assertThat(result).isFalse();
    }
}
