package com.cristiane.salon.models.product.specification;

import com.cristiane.salon.models.product.dto.ProductFilter;
import com.cristiane.salon.models.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {

    private ProductSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Product> filter(ProductFilter filter) {
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
