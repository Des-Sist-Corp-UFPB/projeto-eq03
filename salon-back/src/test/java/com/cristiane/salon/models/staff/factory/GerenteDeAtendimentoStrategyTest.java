package com.cristiane.salon.models.staff.factory;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.models.employee.entity.CommissionScope;
import com.cristiane.salon.models.employee.entity.RemunerationType;
import com.cristiane.salon.models.staff.dto.StaffProfileRequest;
import com.cristiane.salon.models.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GerenteDeAtendimentoStrategyTest {

    private final GerenteDeAtendimentoStrategy strategy = new GerenteDeAtendimentoStrategy();

    private StaffProfileRequest requestWith(RemunerationType type) {
        return new StaffProfileRequest(
                "Ana", "ana@example.com", "Senha@123", "GERENTE_DE_ATENDIMENTO",
                "Ana Souza", null, "111.444.777-35", LocalDate.of(1988, 1, 1), null,
                "81999998888", null, null,
                "50000-000", "Rua A", "10", null, "Boa Vista", "Recife", null,
                null, null,
                LocalDate.now(), null,
                type, type != null ? CommissionScope.INDIVIDUAL : null,
                type != null ? new BigDecimal("100") : null, null, null
        );
    }

    @Test
    void getRoleName_shouldBeGerenteDeAtendimento() {
        assertThat(strategy.getRoleName()).isEqualTo("GERENTE_DE_ATENDIMENTO");
    }

    @Test
    void validate_whenNoRemunerationFieldsSet_shouldNotThrow() {
        strategy.validate(requestWith(null));
    }

    @Test
    void validate_whenRemunerationTypeSet_shouldThrow() {
        assertThatThrownBy(() -> strategy.validate(requestWith(RemunerationType.SALARIO_FIXO)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("não se aplicam ao papel de gerente");
    }

    @Test
    void onStaffCreated_shouldNotThrowAndDoesNothing() {
        User user = new User();
        user.setId(1L);
        strategy.onStaffCreated(user, requestWith(null));
        // Nada a verificar além de não lançar: GERENTE não gera nenhum registro adicional.
    }
}
