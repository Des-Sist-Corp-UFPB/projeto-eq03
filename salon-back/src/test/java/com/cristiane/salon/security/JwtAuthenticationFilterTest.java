package com.cristiane.salon.security;

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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldProceedChain_whenHeaderIsNull() throws ServletException, IOException {
        // Arrange
        request.removeHeader("Authorization");
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldProceedChain_whenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Basic c29tZXRva2Vu");
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldProceedChain_whenUsernameExtractedIsNull() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer invalidtoken");
        when(jwtService.extractUsername("invalidtoken")).thenReturn(null);
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldProceedChain_whenAuthenticationAlreadyExists() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer token");
        org.springframework.security.core.Authentication existingAuth = mock(org.springframework.security.core.Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername("token")).thenReturn("user@salon.com");
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenTokenIsInvalid() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer token");
        UserDetails userDetails = mock(UserDetails.class);
        when(jwtService.extractUsername("token")).thenReturn("user@salon.com");
        when(userDetailsService.loadUserByUsername("user@salon.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token", userDetails)).thenReturn(false);
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldAuthenticate_whenTokenIsValid() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer token");
        UserDetails userDetails = mock(UserDetails.class);
        doReturn(Collections.emptyList()).when(userDetails).getAuthorities();
        when(jwtService.extractUsername("token")).thenReturn("user@salon.com");
        when(userDetailsService.loadUserByUsername("user@salon.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token", userDetails)).thenReturn(true);
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
    }

    @Test
    void doFilterInternal_shouldCatchExceptionAndProceedChain_whenExtractUsernameThrows() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractUsername("token")).thenThrow(new RuntimeException("JWT Parsing Error"));
        FilterChain mockChain = mock(FilterChain.class);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, mockChain);

        // Assert
        verify(mockChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
