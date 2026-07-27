package com.cristiane.salon.models.staff.dto;

import com.cristiane.salon.models.staff.entity.StaffProfile;
import com.cristiane.salon.models.staff.enums.BrazilianState;
import com.cristiane.salon.models.staff.enums.Gender;
import com.cristiane.salon.models.staff.enums.PixKeyType;

import java.time.LocalDate;
import java.time.Instant;

/**
 * Visão de um cadastro da equipe para a API.
 *
 * <p><strong>Este DTO nunca carrega CPF nem chave PIX em texto claro.</strong> O CPF sai
 * mascarado ({@code ***.***.789-01}) e a chave PIX sai só na máscara pré-calculada. Para
 * pagar alguém, use o endpoint de QR Code — que decifra a chave em memória e devolve apenas
 * o payload do PIX, sem nunca expor a chave.
 *
 * <p>A construção passa obrigatoriamente por {@link #fromEntity(StaffProfile)}, para não
 * existir caminho em que alguém monte a response com os valores crus por engano.
 */
public record StaffProfileResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String roleName,
        Boolean active,

        String fullName,
        String socialName,
        String displayName,
        /** Sempre mascarado — o CPF completo não trafega pela API. */
        String cpfMasked,
        LocalDate birthDate,
        Gender gender,

        String phone,
        String emergencyContactName,
        String emergencyContactPhone,

        String zipCode,
        String street,
        String streetNumber,
        String complement,
        String district,
        String city,
        BrazilianState stateUf,

        PixKeyType pixKeyType,
        /** Máscara da chave PIX (ex.: "joa•••••@mail.com"). A chave real nunca sai daqui. */
        String pixKeyMasked,
        boolean hasPixKey,

        LocalDate hiredAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static StaffProfileResponse fromEntity(StaffProfile profile) {
        return new StaffProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getUser().getEmail(),
                profile.getUser().getRoleName(),
                profile.getUser().getActive(),
                profile.getFullName(),
                profile.getSocialName(),
                profile.getDisplayName(),
                maskCpf(profile.getCpf()),
                profile.getBirthDate(),
                profile.getGender(),
                profile.getPhone(),
                profile.getEmergencyContactName(),
                profile.getEmergencyContactPhone(),
                profile.getZipCode(),
                profile.getStreet(),
                profile.getStreetNumber(),
                profile.getComplement(),
                profile.getDistrict(),
                profile.getCity(),
                profile.getStateUf(),
                profile.getPixKeyType(),
                profile.getPixKeyMasked(),
                profile.hasPixKey(),
                profile.getHiredAt(),
                profile.getNotes(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    /** Mostra só os 5 últimos dígitos: {@code ***.***.789-01}. */
    static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***";
        }
        return "***.***." + digits.substring(6, 9) + "-" + digits.substring(9);
    }
}
