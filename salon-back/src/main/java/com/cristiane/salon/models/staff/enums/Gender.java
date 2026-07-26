package com.cristiane.salon.models.staff.enums;

/**
 * Gênero da pessoa, opcional no cadastro.
 *
 * <p>Coletamos gênero (e não orientação sexual) de propósito: orientação sexual é dado
 * pessoal sensível pela LGPD (Art. 5º, II) e não teria finalidade legítima num cadastro
 * de equipe. Gênero se justifica pelo tratamento/pronome correto no atendimento, e ainda
 * assim é opcional — {@link #PREFIRO_NAO_INFORMAR} existe para quem não quiser declarar.
 */
public enum Gender {
    FEMININO,
    MASCULINO,
    NAO_BINARIO,
    OUTRO,
    PREFIRO_NAO_INFORMAR
}
