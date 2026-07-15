package com.cristiane.salon.mcp.security;

import com.cristiane.salon.models.ai.entity.McpAccessToken;
import com.cristiane.salon.models.ai.service.McpTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpAuthenticationFilterTest {

    @Mock
    private McpTokenService mcpTokenService;

    @Mock
    private FilterChain filterChain;

    private McpAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new McpAuthenticationFilter(mcpTokenService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withValidMcpToken_setsMcpClientAuthority() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer mcp_validtoken123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        McpAccessToken token = McpAccessToken.builder().id(42L).revoked(false).build();
        when(mcpTokenService.validateAndTouch("mcp_validtoken123")).thenReturn(Optional.of(token));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("42");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_MCP_CLIENT");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withInvalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer mcp_invalidtoken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(mcpTokenService.validateAndTouch("mcp_invalidtoken")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withNonMcpBearerToken_skipsValidationEntirely() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(mcpTokenService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withNoAuthorizationHeader_doesNotThrow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(mcpTokenService);
        verify(filterChain).doFilter(request, response);
    }
}
