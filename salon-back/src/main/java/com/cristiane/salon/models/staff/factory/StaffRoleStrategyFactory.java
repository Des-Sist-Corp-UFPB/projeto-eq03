package com.cristiane.salon.models.staff.factory;

import com.cristiane.salon.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve a strategy do papel informado.
 *
 * <p>O Spring injeta todas as implementações de {@link StaffRoleStrategy} disponíveis, então
 * criar um papel novo é só adicionar um {@code @Component} — nada aqui precisa mudar.
 *
 * <p>Funciona também como allow-list: um papel sem strategy é rejeitado. É isso que impede
 * que alguém crie um ADMIN ou SYSADMIN por este endpoint mandando outro {@code roleName}.
 */
@Component
public class StaffRoleStrategyFactory {

    private final Map<String, StaffRoleStrategy> strategiesByRole;

    public StaffRoleStrategyFactory(List<StaffRoleStrategy> strategies) {
        this.strategiesByRole = strategies.stream()
                .collect(Collectors.toMap(StaffRoleStrategy::getRoleName, Function.identity()));
    }

    public StaffRoleStrategy resolve(String roleName) {
        StaffRoleStrategy strategy = strategiesByRole.get(roleName);
        if (strategy == null) {
            throw new BadRequestException(
                    "Este cadastro só está disponível para os papéis: " + getSupportedRoles());
        }
        return strategy;
    }

    public boolean supports(String roleName) {
        return strategiesByRole.containsKey(roleName);
    }

    public String getSupportedRoles() {
        return strategiesByRole.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
