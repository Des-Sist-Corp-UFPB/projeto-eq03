package com.cristiane.salon.security;

import com.cristiane.salon.models.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("verifyUserPermissions")
@RequiredArgsConstructor
public class VerifyUserPermissions {

    private final CustomPermissionEvaluator permissionEvaluator;
    private final HttpServletRequest request;

    public boolean userOwnResourceOrHasPermission(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) {
            return false;
        }
        User logged = (User) principal;

        // Give ADMIN full access to everything
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Check explicit permission via endpoint/method
        if (permissionEvaluator.hasPermission(auth, request.getRequestURI(), request.getMethod())) {
            return true;
        }

        // For DELETE requests, explicit permission is required
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        // For POST requests without explicit permission, deny
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        // For GET/PATCH/PUT on specific resource, check if user owns it (userId provided)
        // If userId is null, it means generic endpoint - already checked by hasPermission above
        if (userId != null) {
            return logged.getId().equals(userId);
        }

        // Generic endpoints (userId=null) on GET/PATCH/PUT need explicit permission
        // If we reach here without explicit permission, deny access
        return false;
    }
}
