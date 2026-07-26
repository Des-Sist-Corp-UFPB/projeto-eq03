package com.cristiane.salon.models.staff.repository;

import com.cristiane.salon.models.staff.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long>,
        JpaSpecificationExecutor<StaffProfile> {

    /**
     * Busca pelo hash do CPF — o campo cifrado não é pesquisável, então a checagem de
     * duplicidade passa por aqui.
     */
    Optional<StaffProfile> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);

    Optional<StaffProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
