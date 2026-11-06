package com.project.rentmebarwaha.property.repository;

import com.project.rentmebarwaha.property.dto.PropertyFilterCriteria;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PropertySpecification {

    public static Specification<Property> build(PropertyFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Rule: Only ACTIVE non-deleted properties appear in public search
            predicates.add(cb.equal(root.get("status"), PropertyStatus.ACTIVE));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // City filter
            if (criteria.city() != null && !criteria.city().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), criteria.city().trim().toLowerCase()));
            }

            // Locality filter
            if (criteria.locality() != null && !criteria.locality().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("locality")), criteria.locality().trim().toLowerCase()));
            }

            // Full text search query 'q' (matches locality, landmark, title, description)
            if (criteria.q() != null && !criteria.q().trim().isEmpty()) {
                String searchPattern = "%" + criteria.q().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate localityMatch = cb.like(cb.lower(root.get("locality")), searchPattern);
                Predicate addressMatch = cb.like(cb.lower(root.get("address")), searchPattern);
                predicates.add(cb.or(titleMatch, localityMatch, addressMatch));
            }

            // Rent range
            if (criteria.minRent() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("monthlyRent"), criteria.minRent()));
            }
            if (criteria.maxRent() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("monthlyRent"), criteria.maxRent()));
            }

            // BHK Configurations (multi-select)
            if (criteria.configs() != null && !criteria.configs().isEmpty()) {
                predicates.add(root.get("config").in(criteria.configs()));
            }

            // Furnishing (multi-select)
            if (criteria.furnishings() != null && !criteria.furnishings().isEmpty()) {
                predicates.add(root.get("furnishing").in(criteria.furnishings()));
            }

            // Preferred tenants
            if (criteria.preferredTenants() != null) {
                predicates.add(cb.equal(root.get("preferredTenants"), criteria.preferredTenants()));
            }

            // Available immediately
            if (criteria.availableImmediately() != null && criteria.availableImmediately()) {
                predicates.add(cb.isTrue(root.get("availableImmediately")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

