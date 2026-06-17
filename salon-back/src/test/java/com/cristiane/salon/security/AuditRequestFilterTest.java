package com.cristiane.salon.security;

import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRequestFilterTest {

    @InjectMocks
    private AuditRequestFilter auditRequestFilter;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldSkipAudit_whenGetRequest() throws ServletException, IOException {
        // Arrange
        request.setMethod("GET");
        request.setRequestURI("/v1/products");

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void doFilterInternal_shouldSkipAudit_whenOptionsRequest() throws ServletException, IOException {
        // Arrange
        request.setMethod("OPTIONS");
        request.setRequestURI("/v1/products");

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void doFilterInternal_shouldSkipAudit_whenUriIsExcluded() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/ping");

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void doFilterInternal_shouldNotLog_whenStatusIs400() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/products");
        response.setStatus(400);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void doFilterInternal_shouldNotLog_whenUserNotAuthenticatedAndStatusNot401Or403() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/products");
        response.setStatus(200);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void doFilterInternal_shouldLog_whenUserNotAuthenticatedAndStatusIs401() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/products");
        response.setStatus(401);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(auditLogService).logAction(
                eq(null),
                eq("GUEST"),
                eq("POST /v1/products"),
                eq("Product"),
                eq(null),
                contains("Status Code: 401"),
                eq("FAILURE"),
                eq("HTTP Status 401")
        );
    }

    @Test
    void doFilterInternal_shouldLog_whenUserIsAuthenticatedAsCustomUser() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/products/123");
        response.setStatus(200);

        User user = new User();
        user.setId(99L);
        user.setEmail("user@salon.com");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(auditLogService).logAction(
                eq(99L),
                eq("user@salon.com"),
                eq("POST /v1/products/123"),
                eq("Product"),
                eq(123L),
                contains("Status Code: 200"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldLog_whenUserIsAuthenticatedAsUserDetails() throws ServletException, IOException {
        // Arrange
        request.setMethod("PUT");
        request.setRequestURI("/v1/appointments");
        response.setStatus(201);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("details@salon.com");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(auditLogService).logAction(
                eq(null),
                eq("details@salon.com"),
                eq("PUT /v1/appointments"),
                eq("Appointment"),
                eq(null),
                contains("Status Code: 201"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldLog_whenUserIsAuthenticatedAsString() throws ServletException, IOException {
        // Arrange
        request.setMethod("PATCH");
        request.setRequestURI("/v1/employees/abc");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("stringuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(auditLogService).logAction(
                eq(null),
                eq("stringuser"),
                eq("PATCH /v1/employees/abc"),
                eq("Employee"),
                eq(null),
                contains("Resource Key: abc | Request Method: PATCH"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldLogFailure_whenFilterChainThrowsException() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/users");
        response.setStatus(200);

        RuntimeException expectedException = new RuntimeException("Filter Chain Error");
        doThrow(expectedException).when(filterChain).doFilter(request, response);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("stringuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act & Assert
        assertThatThrownBy(() -> auditRequestFilter.doFilter(request, response, filterChain))
                .isSameAs(expectedException);

        verify(auditLogService).logAction(
                eq(null),
                eq("stringuser"),
                eq("POST /v1/users"),
                eq("User"),
                eq(null),
                contains("Request Method: POST"),
                eq("FAILURE"),
                eq("Filter Chain Error")
        );
    }

    @Test
    void doFilterInternal_shouldHandleSysadminUris() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/sysadmin/feature-flags/my-flag/toggle");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("sysuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(auditLogService).logAction(
                eq(null),
                eq("sysuser"),
                eq("POST /v1/sysadmin/feature-flags/my-flag/toggle"),
                eq("FeatureFlag"),
                eq(null),
                contains("Resource Key: my-flag | Operation: toggle"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldHandleSysadminUrisWithoutSegments() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/sysadmin");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("sysuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(auditLogService).logAction(
                eq(null),
                eq("sysuser"),
                eq("POST /v1/sysadmin"),
                eq("Sysadmin"),
                eq(null),
                contains("Request Method: POST"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldHandleRootUrisAndTrailingSlash() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/reports/");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("sysuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(auditLogService).logAction(
                eq(null),
                eq("sysuser"),
                eq("POST /v1/reports/"),
                eq("Report"),
                eq(null),
                contains("Request Method: POST"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldHandleEntityMappingOtherCases() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/cashflow");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("sysuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(auditLogService).logAction(
                eq(null),
                eq("sysuser"),
                eq("POST /v1/cashflow"),
                eq("CashFlow"),
                eq(null),
                contains("Request Method: POST"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldHandleEntityMappingAuth() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/auth/logout");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("sysuser");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(auditLogService).logAction(
                eq(null),
                eq("sysuser"),
                eq("POST /v1/auth/logout"),
                eq("Auth"),
                eq(null),
                contains("Resource Key: logout |"),
                eq("SUCCESS"),
                eq(null)
        );
    }

    @Test
    void doFilterInternal_shouldCatchInternalExceptionsSilently() throws ServletException, IOException {
        // Arrange
        request.setMethod("POST");
        request.setRequestURI("/v1/users");
        response.setStatus(200);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Force logAction to throw an exception
        doThrow(new RuntimeException("Audit service down")).when(auditLogService).logAction(
                any(), any(), any(), any(), any(), any(), any(), any()
        );

        // Act
        auditRequestFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(auditLogService).logAction(any(), any(), any(), any(), any(), any(), any(), any());
        // verify no exceptions are thrown outside the filter
    }
}
