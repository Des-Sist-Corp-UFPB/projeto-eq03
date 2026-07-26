package com.cristiane.salon.models.salonprofile.repository;

import com.cristiane.salon.models.salonprofile.entity.SalonProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonProfileRepository extends JpaRepository<SalonProfile, Long> {
    Optional<SalonProfile> findFirstByOrderByIdAsc();
}
