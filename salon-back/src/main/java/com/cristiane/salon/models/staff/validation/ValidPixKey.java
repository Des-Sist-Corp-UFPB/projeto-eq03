package com.cristiane.salon.models.staff.validation;

import com.cristiane.salon.models.staff.dto.StaffProfileRequest;
import com.cristiane.salon.models.staff.enums.PixKeyType;
import com.cristiane.salon.utils.CpfValidator;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validação cruzada da chave PIX: precisa ser aplicada no objeto inteiro (e não num campo)
 * porque o formato válido da chave depende do tipo declarado.
 *
 * <p>Regras:
 * <ul>
 *   <li>tipo e chave são opcionais, mas ou vêm os dois ou nenhum;</li>
 *   <li>a chave tem que casar com o formato do tipo declarado;</li>
 *   <li>se o tipo for CPF, os dígitos verificadores também são checados.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = ValidPixKey.Validator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPixKey {

    String message() default "Chave PIX inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPixKey, StaffProfileRequest> {

        @Override
        public boolean isValid(StaffProfileRequest request, ConstraintValidatorContext context) {
            if (request == null) {
                return true;
            }

            PixKeyType type = request.pixKeyType();
            String key = request.pixKey();
            boolean hasType = type != null;
            boolean hasKey = key != null && !key.isBlank();

            if (!hasType && !hasKey) {
                return true;
            }

            context.disableDefaultConstraintViolation();

            if (hasType != hasKey) {
                String field = hasType ? "pixKey" : "pixKeyType";
                String message = hasType
                        ? "Informe a chave PIX correspondente ao tipo selecionado"
                        : "Informe o tipo da chave PIX";
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(field)
                        .addConstraintViolation();
                return false;
            }

            if (!type.matches(key)) {
                context.buildConstraintViolationWithTemplate(type.getMessage())
                        .addPropertyNode("pixKey")
                        .addConstraintViolation();
                return false;
            }

            if (type == PixKeyType.CPF && !CpfValidator.isValid(key)) {
                context.buildConstraintViolationWithTemplate("O CPF informado como chave PIX é inválido")
                        .addPropertyNode("pixKey")
                        .addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
