package com.cristiane.salon.aspect;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    @AfterReturning(
            pointcut = "@annotation(auditable)",
            returning = "result"
    )
    public void logSuccessfulAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Long userId = getUserId();
            String userEmail = getUserEmail();
            String action = auditable.action();
            String entityType = auditable.entityType();
            
            // Check status override for DONE appointments
            if ("APPOINTMENT_STATUS_CHANGED".equals(action)) {
                boolean isDone = false;
                try {
                    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                    String[] parameterNames = signature.getParameterNames();
                    Object[] args = joinPoint.getArgs();
                    if (parameterNames != null && args != null) {
                        for (int i = 0; i < parameterNames.length; i++) {
                            if ("status".equals(parameterNames[i]) && args[i] instanceof String statusVal) {
                                if ("DONE".equalsIgnoreCase(statusVal)) {
                                    isDone = true;
                                }
                                break;
                            }
                        }
                    } else if (args != null) {
                        for (Object arg : args) {
                            if (arg instanceof String statusVal && "DONE".equalsIgnoreCase(statusVal)) {
                                isDone = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                
                if (isDone) {
                    action = "APPOINTMENT_COMPLETED";
                }
            }
            
            Long entityId = extractEntityId(joinPoint, result);
            String details = auditable.captureArgs() ? extractDetails(joinPoint) : null;
            
            auditLogService.logAction(
                    userId,
                    userEmail,
                    action,
                    entityType,
                    entityId,
                    details,
                    "SUCCESS"
            );
        } catch (Exception e) {
            // Log silenciosamente para não quebrar a aplicação
        }
    }
    
    @AfterThrowing(
            pointcut = "@annotation(auditable)",
            throwing = "exception"
    )
    public void logFailedAction(JoinPoint joinPoint, Auditable auditable, Exception exception) {
        try {
            Long userId = getUserId();
            String userEmail = getUserEmail();
            String action = auditable.action();
            String entityType = auditable.entityType();
            Long entityId = extractEntityId(joinPoint, null);
            
            auditLogService.logAction(
                    userId,
                    userEmail,
                    action,
                    entityType,
                    entityId,
                    null,
                    "FAILURE",
                    exception.getMessage()
            );
        } catch (Exception e) {
            // Log silenciosamente para não quebrar a aplicação
        }
    }
    
    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.cristiane.salon.models.user.entity.User user) {
            return user.getId();
        }
        return null;
    }
    
    private String getUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "SYSTEM";
    }
    
    private Long extractEntityId(JoinPoint joinPoint, Object result) {
        // 1. Try to extract from PathVariable (for update/delete)
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof PathVariable) {
                        if (args[i] instanceof Number) {
                            return ((Number) args[i]).longValue();
                        } else if (args[i] instanceof String) {
                            try {
                                return Long.parseLong((String) args[i]);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Try to extract from return value (for creation - POST)
        if (result != null) {
            Object body = result;
            if (result instanceof ResponseEntity<?> responseEntity) {
                body = responseEntity.getBody();
            }
            if (body != null) {
                if (body instanceof Number) {
                    return ((Number) body).longValue();
                }
                try {
                    Method idMethod = body.getClass().getMethod("id");
                    Object idVal = idMethod.invoke(body);
                    if (idVal instanceof Number) {
                        return ((Number) idVal).longValue();
                    }
                } catch (Exception ignored) {
                    try {
                        Method getIdMethod = body.getClass().getMethod("getId");
                        Object idVal = getIdMethod.invoke(body);
                        if (idVal instanceof Number) {
                            return ((Number) idVal).longValue();
                        }
                    } catch (Exception ignored2) {
                        try {
                            Field idField = body.getClass().getDeclaredField("id");
                            idField.setAccessible(true);
                            Object idVal = idField.get(body);
                            if (idVal instanceof Number) {
                                return ((Number) idVal).longValue();
                            }
                        } catch (Exception ignored3) {}
                    }
                }
            }
        }

        // 3. Fallback: search for any Long argument in method signature
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof Long) {
                    return (Long) arg;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }
    
    private String extractDetails(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            List<Object> maskedArgs = new ArrayList<>();
            for (Object arg : args) {
                maskedArgs.add(maskSensitiveFields(arg));
            }
            return objectMapper.writeValueAsString(maskedArgs);
        } catch (Exception e) {
            return null;
        }
    }

    private Object maskSensitiveFields(Object arg) {
        if (arg == null) {
            return null;
        }
        
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean || 
            arg instanceof java.time.temporal.Temporal || arg instanceof java.util.Date) {
            return arg;
        }
        
        try {
            Map<String, Object> map = objectMapper.convertValue(arg, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            maskMap(map);
            return map;
        } catch (Exception e) {
            return arg;
        }
    }
    
    private void maskMap(Map<String, Object> map) {
        if (map == null) return;
        
        Set<String> sensitiveKeys = Set.of(
            "password", "senha", "token", "jwt", "refreshtoken", "clientnotes", "client_notes",
            "card", "cartao", "creditcard", "bank", "banco", "cvv", "cardNumber", "card_number",
            "apikey", "api_key", "accesstoken", "access_token", "secret"
        );
        
        for (Map.Entry<String, Object> entry : new HashSet<>(map.entrySet())) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            
            if (sensitiveKeys.contains(key)) {
                map.put(entry.getKey(), "***");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) value;
                maskMap(childMap);
            } else if (value instanceof Collection) {
                for (Object item : (Collection<?>) value) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> childMap = (Map<String, Object>) item;
                        maskMap(childMap);
                    }
                }
            }
        }
    }
}
