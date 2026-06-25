package com.cristiane.salon.security;

import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRequestFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Exclude public docs, swagger, CORS pre-flights, manual auth logins, and audit log endpoints to avoid loops/duplicates
        if (shouldSkipAudit(uri, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Exception exception = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            try {
                // Determine status and error message
                int status = response.getStatus();
                
                // Erros de validação de payload (400 Bad Request) — não geram audit log
                if (status == 400) {
                    return;
                }

                String auditStatus = (status >= 400 || exception != null) ? "FAILURE" : "SUCCESS";
                String errorMessage = exception != null ? exception.getMessage() : 
                                      (status >= 400 ? "HTTP Status " + status : null);

                // Get authenticated user (post-execution of filter chain)
                Long userId = null;
                String userEmail = "GUEST";
                boolean isAuthenticated = false;
                
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && 
                    !"anonymousUser".equals(authentication.getPrincipal().toString())) {
                    isAuthenticated = true;
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof User user) {
                        userId = user.getId();
                        userEmail = user.getEmail();
                    } else if (principal instanceof UserDetails userDetails) {
                        userEmail = userDetails.getUsername();
                    } else if (principal instanceof String) {
                        userEmail = (String) principal;
                    }
                }

                // Tentativas de acesso a endpoints públicos sem autenticação não geram logs
                if (!isAuthenticated && status != 401 && status != 403) {
                    return;
                }

                // Determine entity type, entity ID, and details prefix from URI segments
                String entityType = "HTTP_REQUEST";
                Long entityId = null;
                String detailPrefix = "";
                
                String path = uri;
                if (path.startsWith("/v1/")) {
                    path = path.substring(4);
                } else if (path.startsWith("/v1")) {
                    path = path.substring(3);
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                String[] segments = path.isEmpty() ? new String[0] : path.split("/");

                if (segments.length > 0) {
                    String firstSegment = segments[0];
                    if ("sysadmin".equalsIgnoreCase(firstSegment)) {
                        if (segments.length > 1) {
                            String secondSegment = segments[1];
                            entityType = getEntityNameFromResource(secondSegment);
                            if (segments.length > 2) {
                                detailPrefix = "Resource Key: " + segments[2] + " | ";
                                if (segments.length > 3) {
                                    detailPrefix += "Operation: " + segments[3] + " | ";
                                }
                            }
                        } else {
                            entityType = "Sysadmin";
                        }
                    } else {
                        entityType = getEntityNameFromResource(firstSegment);
                        if (segments.length > 1) {
                            String secondSegment = segments[1];
                            try {
                                entityId = Long.parseLong(secondSegment);
                            } catch (NumberFormatException ignored) {
                                detailPrefix = "Resource Key: " + secondSegment + " | ";
                            }
                        }
                    }
                }

                String action = method + " " + uri;
                String details = detailPrefix + String.format("Request Method: %s | Path: %s | Status Code: %d", method, uri, status);

                auditLogService.logAction(
                        userId,
                        userEmail,
                        action,
                        entityType,
                        entityId,
                        details,
                        auditStatus,
                        errorMessage
                );
                log.info("Audit Logged: User={} | Action={} | EntityType={} | Status={}", userEmail, action, entityType, auditStatus);
            } catch (Exception e) {
                log.error("Error logging HTTP request to audit log", e);
            }
        }
    }

    private boolean shouldSkipAudit(String uri, String method) {
        if ("OPTIONS".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method)) {
            return true;
        }
        
        return "/ping".equals(uri) ||
               uri.contains("/swagger-ui") || 
               uri.contains("/v3/api-docs") || 
               uri.contains("/swagger-resources") || 
               uri.contains("/webjars") || 
               uri.startsWith("/v1/audit") ||
               uri.contains("/auth/login") ||
               uri.contains("/auth/register");
    }

    private String getEntityNameFromResource(String resource) {
        if (resource == null) return "HTTP_REQUEST";
        
        return switch (resource.toLowerCase()) {
            case "services" -> "Service";
            case "appointments" -> "Appointment";
            case "employees" -> "Employee";
            case "products" -> "Product";
            case "users" -> "User";
            case "auth" -> "Auth";
            case "feature-flags" -> "FeatureFlag";
            case "reports" -> "Report";
            case "cashflow" -> "CashFlow";
            default -> Character.toUpperCase(resource.charAt(0)) + 
                       (resource.length() > 1 ? resource.substring(1) : "");
        };
    }
}
