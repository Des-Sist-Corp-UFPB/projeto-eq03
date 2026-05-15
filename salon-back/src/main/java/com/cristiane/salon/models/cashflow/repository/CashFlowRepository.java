package com.cristiane.salon.models.cashflow.repository;

import com.cristiane.salon.models.cashflow.entity.CashFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {
    
    @Query("SELECT c FROM CashFlow c WHERE c.date >= :startDate AND c.date <= :endDate ORDER BY c.date DESC")
    List<CashFlow> findByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
