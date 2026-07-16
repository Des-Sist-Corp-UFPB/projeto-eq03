package com.cristiane.salon.models.employee.specification;

import com.cristiane.salon.models.employee.dto.EmployeeFilter;
import com.cristiane.salon.models.employee.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecifications {

    private EmployeeSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Employee> filter(EmployeeFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.name() != null && !filter.name().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.join("user").get("name")),
                        "%" + filter.name().toLowerCase() + "%"
                    ));
                }

                if (filter.active() != null) {
                    predicates.add(criteriaBuilder.equal(root.join("user").get("active"), filter.active()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
