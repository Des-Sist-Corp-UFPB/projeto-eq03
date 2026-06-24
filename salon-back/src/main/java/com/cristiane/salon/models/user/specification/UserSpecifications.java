package com.cristiane.salon.models.user.specification;

import com.cristiane.salon.models.user.dto.ClientFilter;
import com.cristiane.salon.models.user.dto.UserFilter;
import com.cristiane.salon.models.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    private UserSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<User> filterClients(ClientFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Enforce role name is 'CLIENTE'
            predicates.add(criteriaBuilder.equal(root.join("role").get("name"), "CLIENTE"));

            if (filter != null) {
                if (filter.name() != null && !filter.name().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.name().toLowerCase() + "%"
                    ));
                }

                if (filter.email() != null && !filter.email().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + filter.email().toLowerCase() + "%"
                    ));
                }

                if (filter.phone() != null && !filter.phone().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("phone")),
                        "%" + filter.phone().toLowerCase() + "%"
                    ));
                }

                if (filter.cpf() != null && !filter.cpf().isBlank()) {
                    String cleanCpf = filter.cpf().replaceAll("\\D", "");
                    predicates.add(criteriaBuilder.equal(root.get("cpf"), cleanCpf));
                }

                if (filter.active() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("active"), filter.active()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<User> filterUsers(UserFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude role name 'CLIENTE'
            predicates.add(criteriaBuilder.notEqual(root.join("role").get("name"), "CLIENTE"));

            if (filter != null) {
                if (filter.name() != null && !filter.name().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.name().toLowerCase() + "%"
                    ));
                }

                if (filter.email() != null && !filter.email().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + filter.email().toLowerCase() + "%"
                    ));
                }

                if (filter.phone() != null && !filter.phone().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("phone")),
                        "%" + filter.phone().toLowerCase() + "%"
                    ));
                }

                if (filter.active() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("active"), filter.active()));
                }

                if (filter.roleId() != null) {
                    predicates.add(criteriaBuilder.equal(root.join("role").get("id"), filter.roleId()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
