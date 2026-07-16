package com.cristiane.salon.models.service.specification;

import com.cristiane.salon.models.service.dto.SalonServiceFilter;
import com.cristiane.salon.models.service.entity.SalonService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SalonServiceSpecifications {

    private SalonServiceSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<SalonService> filter(SalonServiceFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.name() != null && !filter.name().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.name().toLowerCase() + "%"
                    ));
                }

                if (filter.active() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("active"), filter.active()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
