package com.cristiane.salon.controllers;

import com.cristiane.salon.config.TestSecurityConfig;
import com.cristiane.salon.mcp.security.McpAuthenticationFilter;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.security.AuditRequestFilter;
import com.cristiane.salon.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class BaseControllerTest {

    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected AuditRequestFilter auditRequestFilter;

    @MockitoBean
    protected McpAuthenticationFilter mcpAuthenticationFilter;

    @MockitoBean
    protected AuditLogService auditLogService;

    @BeforeEach
    void setUpFilters() throws Exception {
        Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(auditRequestFilter).doFilter(any(), any(), any());

        Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(mcpAuthenticationFilter).doFilter(any(), any(), any());
    }
}