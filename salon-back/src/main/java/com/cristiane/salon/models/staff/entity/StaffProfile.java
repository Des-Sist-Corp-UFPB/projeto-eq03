package com.cristiane.salon.models.staff.entity;

import com.cristiane.salon.models.staff.enums.BrazilianState;
import com.cristiane.salon.models.staff.enums.Gender;
import com.cristiane.salon.models.staff.enums.PixKeyType;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.security.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.Instant;

/**
 * Cadastro completo de um membro da equipe (FUNCIONARIA ou GERENTE_DE_ATENDIMENTO).
 *
 * <p>Separado de {@link User} (autenticação) e de Employee (remuneração) para isolar os
 * dados pessoais sensíveis numa tabela própria.
 *
 * <p>CPF e chave PIX são cifrados de forma transparente pelo {@link EncryptedStringConverter}
 * — em memória são texto claro, no banco são ciphertext AES-256-GCM. Nunca devolva estes
 * campos direto numa response: use as versões mascaradas do DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_staff_profile")
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // --- Dados pessoais ---

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Nome social: como a pessoa quer ser chamada, se diferente do nome de registro. */
    @Column(name = "social_name", length = 150)
    private String socialName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "cpf_encrypted", nullable = false, columnDefinition = "TEXT")
    private String cpf;

    /** HMAC do CPF só com dígitos — permite UNIQUE/busca por igualdade sem decifrar. */
    @Column(name = "cpf_hash", nullable = false, unique = true, length = 64)
    private String cpfHash;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30)
    private Gender gender;

    // --- Contato ---

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    // --- Endereço ---

    @Column(name = "zip_code", nullable = false, length = 9)
    private String zipCode;

    @Column(name = "street", nullable = false, length = 200)
    private String street;

    @Column(name = "street_number", nullable = false, length = 20)
    private String streetNumber;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_uf", nullable = false, length = 2)
    private BrazilianState stateUf;

    // --- Recebimento via PIX ---

    @Enumerated(EnumType.STRING)
    @Column(name = "pix_key_type", length = 20)
    private PixKeyType pixKeyType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pix_key_encrypted", columnDefinition = "TEXT")
    private String pixKey;

    /** Máscara pré-calculada para a UI, evitando decifrar a chave só para exibir. */
    @Column(name = "pix_key_masked", length = 120)
    private String pixKeyMasked;

    // --- Metadados ---

    @Column(name = "hired_at")
    private LocalDate hiredAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean hasPixKey() {
        return pixKeyType != null && pixKey != null && !pixKey.isBlank();
    }

    /** Nome social quando informado, senão o nome completo. */
    public String getDisplayName() {
        return socialName != null && !socialName.isBlank() ? socialName : fullName;
    }
}
