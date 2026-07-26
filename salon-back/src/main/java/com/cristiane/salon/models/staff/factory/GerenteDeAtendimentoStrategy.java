package com.cristiane.salon.models.staff.factory;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.models.staff.dto.StaffProfileRequest;
import com.cristiane.salon.models.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * GERENTE_DE_ATENDIMENTO não atende clientes diretamente, então não vira Employee e não
 * tem comissão — recusa explicitamente campos de remuneração em vez de ignorá-los em
 * silêncio, para o formulário não passar a impressão de que foram salvos.
 */
@Component
public class GerenteDeAtendimentoStrategy implements StaffRoleStrategy {

    @Override
    public String getRoleName() {
        return "GERENTE_DE_ATENDIMENTO";
    }

    @Override
    public void validate(StaffProfileRequest request) {
        if (request.remunerationType() != null
                || request.commissionScope() != null
                || request.remunerationValue() != null
                || request.commissionValue() != null) {
            throw new BadRequestException(
                    "Dados de remuneração e comissão não se aplicam ao papel de gerente de atendimento");
        }
    }

    @Override
    public void onStaffCreated(User user, StaffProfileRequest request) {
        // Nada a fazer: o perfil e o usuário já cobrem tudo que este papel precisa.
    }
}
