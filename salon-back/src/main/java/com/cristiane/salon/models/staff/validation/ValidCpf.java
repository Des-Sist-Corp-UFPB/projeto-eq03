package com.cristiane.salon.models.staff.validation;

import com.cristiane.salon.utils.CpfValidator;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valida CPF de verdade — não só o formato, mas os dígitos verificadores, reusando o
 * {@link CpfValidator} que já existia no projeto.
 */
@Documented
@Constraint(validatedBy = ValidCpf.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCpf {

    String message() default "CPF inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCpf, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Nulo/vazio é responsabilidade do @NotBlank — aqui só valida o que veio preenchido.
            if (value == null || value.isBlank()) {
                return true;
            }
            return CpfValidator.isValid(value);
        }
    }
}
