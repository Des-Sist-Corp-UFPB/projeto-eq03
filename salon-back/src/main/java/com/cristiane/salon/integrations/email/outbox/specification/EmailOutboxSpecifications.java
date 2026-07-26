package com.cristiane.salon.integrations.email.outbox.specification;

import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxFilter;
import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EmailOutboxSpecifications {

    private EmailOutboxSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<EmailOutboxEntry> filter(EmailOutboxFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null && filter.statuses() != null && !filter.statuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.statuses()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
