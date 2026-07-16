package com.cristiane.salon.models.appointment.specification;

import com.cristiane.salon.models.appointment.dto.AppointmentFilter;
import com.cristiane.salon.models.appointment.entity.Appointment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AppointmentSpecifications {

    private AppointmentSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Appointment> filter(AppointmentFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.status() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
                }

                if (filter.paymentStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), filter.paymentStatus()));
                }

                if (filter.employeeId() != null) {
                    predicates.add(criteriaBuilder.equal(root.join("employee").get("id"), filter.employeeId()));
                }

                if (filter.clientId() != null) {
                    predicates.add(criteriaBuilder.equal(root.join("client").get("id"), filter.clientId()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
