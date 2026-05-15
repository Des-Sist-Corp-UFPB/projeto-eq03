package com.cristiane.salon.models.service.repository;

import com.cristiane.salon.models.service.entity.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalonServiceRepository extends JpaRepository<Service, Long> {
}
