package com.cristiane.salon.models.staff.factory;

import com.cristiane.salon.models.staff.dto.StaffProfileRequest;
import com.cristiane.salon.models.user.entity.User;

/**
 * Regras que variam conforme o papel do membro da equipe.
 *
 * <p>Existe porque o formulário muda de acordo com o papel escolhido: FUNCIONARIA precisa de
 * dados de remuneração e gera um registro de Employee; GERENTE_DE_ATENDIMENTO não. Deixar isso
 * numa strategy por papel (em vez de um {@code if} no service) mantém a regra de cada papel
 * num lugar só e torna trivial adicionar um papel novo depois.
 */
public interface StaffRoleStrategy {

    /** Papel que esta strategy atende, igual ao {@code name} em tb_role. */
    String getRoleName();

    /**
     * Valida os campos que só fazem sentido para este papel.
     *
     * @throws com.cristiane.salon.exception.BadRequestException se algo obrigatório faltar
     */
    void validate(StaffProfileRequest request);

    /**
     * Cria os registros extras que este papel exige (ex.: Employee para FUNCIONARIA).
     * Roda dentro da mesma transação da criação do usuário e do perfil.
     */
    void onStaffCreated(User user, StaffProfileRequest request);
}
