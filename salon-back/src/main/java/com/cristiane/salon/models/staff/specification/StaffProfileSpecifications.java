package com.cristiane.salon.models.staff.specification;

import com.cristiane.salon.models.staff.dto.StaffFilter;
import com.cristiane.salon.models.staff.entity.StaffProfile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StaffProfileSpecifications {

    private StaffProfileSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<StaffProfile> filter(StaffFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.name() != null && !filter.name().isBlank()) {
                    String pattern = "%" + filter.name().toLowerCase() + "%";
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("socialName")), pattern)
                    ));
                }

                if (filter.roleName() != null && !filter.roleName().isBlank()) {
                    predicates.add(criteriaBuilder.equal(
                            root.join("user").join("role").get("name"), filter.roleName()));
                }

                if (filter.active() != null) {
                    predicates.add(criteriaBuilder.equal(root.join("user").get("active"), filter.active()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
