package com.cristiane.salon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuração de segurança de método para habilitar as anotações {@code @PreAuthorize}.
 * 
 * Padrão de Autorização Adotado:
 * - A autorização de endpoints/métodos de serviço é realizada de forma customizada via expressões SpEL
 *   em anotações, como por exemplo: {@code @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(#id)")}.
 * - O bean {@code verifyUserPermissions} (da classe {@link com.cristiane.salon.security.VerifyUserPermissions})
 *   avalia e resolve dinamicamente se o acesso deve ser permitido:
 *   1. Permite acesso total para as roles {@code ROLE_SYSADMIN} e {@code ROLE_ADMIN} (com restrições específicas para rotas sysadmin/audit).
 *   2. Valida se o usuário logado é dono do recurso (comparando o ID do usuário autenticado com o ID informado no recurso).
 *   3. Delega a verificação de permissões dinâmicas associadas ao método HTTP e endpoint da requisição atual 
 *      através do {@link com.cristiane.salon.security.CustomPermissionEvaluator}.
 * 
 * Nota: O mecanismo de {@code PermissionEvaluator} padrão do Spring Security (usando expressões {@code hasPermission(...)})
 * não é utilizado nesta arquitetura, em favor do padrão customizado e centralizado. Portanto, a classe
 * {@code EntityPermissionEvaluator} foi completamente removida como código morto.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
