package com.cristiane.salon.models.staff.service;

import com.cristiane.salon.exception.ConflictException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.staff.dto.StaffFilter;
import com.cristiane.salon.models.staff.dto.StaffProfileRequest;
import com.cristiane.salon.models.staff.dto.StaffProfileResponse;
import com.cristiane.salon.models.staff.entity.StaffProfile;
import com.cristiane.salon.models.staff.factory.StaffRoleStrategy;
import com.cristiane.salon.models.staff.factory.StaffRoleStrategyFactory;
import com.cristiane.salon.models.staff.repository.StaffProfileRepository;
import com.cristiane.salon.models.staff.specification.StaffProfileSpecifications;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.RoleRepository;
import com.cristiane.salon.models.user.repository.UserRepository;
import com.cristiane.salon.security.crypto.PiiHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro completo de um membro da equipe: cria o {@code User} (login), o
 * {@code StaffProfile} (dados pessoais/endereço/PIX) e, conforme o papel, os registros
 * adicionais que ele exigir (ex.: Employee para FUNCIONARIA) — tudo numa única transação.
 *
 * <p>Só ADMIN/SYSADMIN chegam aqui (ver {@code @PreAuthorize} no controller): não existe
 * autocadastro para estes papéis, de propósito — dados como remuneração e chave PIX não
 * devem ser preenchidos pela própria pessoa sem revisão.
 */
@Service
@RequiredArgsConstructor
public class StaffProfileService {

    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PiiHashUtil piiHashUtil;
    private final StaffRoleStrategyFactory strategyFactory;

    @Transactional
    public StaffProfileResponse create(StaffProfileRequest request) {
        StaffRoleStrategy strategy = strategyFactory.resolve(request.roleName());
        strategy.validate(request);

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email já está em uso");
        }

        String cpfDigits = request.cpfDigitsOnly();
        String cpfHash = piiHashUtil.hash(cpfDigits);
        if (staffProfileRepository.existsByCpfHash(cpfHash)) {
            throw new ConflictException("Já existe um cadastro de equipe com este CPF");
        }

        Role role = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> new ResourceNotFoundException("Papel não encontrado: " + request.roleName()));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(role);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        StaffProfile profile = new StaffProfile();
        profile.setUser(savedUser);
        profile.setFullName(request.fullName());
        profile.setSocialName(request.socialName());
        profile.setCpf(cpfDigits);
        profile.setCpfHash(cpfHash);
        profile.setBirthDate(request.birthDate());
        profile.setGender(request.gender());
        profile.setPhone(request.phone());
        profile.setEmergencyContactName(request.emergencyContactName());
        profile.setEmergencyContactPhone(request.emergencyContactPhone());
        profile.setZipCode(request.normalizedZipCode());
        profile.setStreet(request.street());
        profile.setStreetNumber(request.streetNumber());
        profile.setComplement(request.complement());
        profile.setDistrict(request.district());
        profile.setCity(request.city());
        profile.setStateUf(request.stateUf());
        profile.setHiredAt(request.hiredAt());
        profile.setNotes(request.notes());
        profile.setCreatedByUserId(getAuthenticatedUserId());

        if (request.pixKeyType() != null) {
            profile.setPixKeyType(request.pixKeyType());
            profile.setPixKey(request.pixKey());
            profile.setPixKeyMasked(maskPixKey(request.pixKey()));
        }

        StaffProfile savedProfile = staffProfileRepository.save(profile);

        strategy.onStaffCreated(savedUser, request);

        return StaffProfileResponse.fromEntity(savedProfile);
    }

    @Transactional(readOnly = true)
    public StaffProfileResponse findById(Long id) {
        return StaffProfileResponse.fromEntity(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<StaffProfileResponse> findAll(StaffFilter filter, Pageable pageable) {
        return staffProfileRepository
                .findAll(StaffProfileSpecifications.filter(filter), pageable)
                .map(StaffProfileResponse::fromEntity);
    }

    private StaffProfile getOrThrow(Long id) {
        return staffProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cadastro de equipe não encontrado"));
    }

    private Long getAuthenticatedUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    /** Mostra só um prefixo e um sufixo curtos, suficientes para reconhecimento, nunca a chave inteira. */
    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.isBlank()) {
            return null;
        }
        int visiblePrefix = Math.min(3, pixKey.length());
        int visibleSuffix = Math.min(4, pixKey.length() - visiblePrefix);
        if (visibleSuffix <= 0) {
            return "•".repeat(pixKey.length());
        }
        return pixKey.substring(0, visiblePrefix) + "•••••" + pixKey.substring(pixKey.length() - visibleSuffix);
    }
}
