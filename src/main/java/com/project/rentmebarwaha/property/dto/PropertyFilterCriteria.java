package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PreferredTenants;
import com.project.rentmebarwaha.property.entity.PropertyConfig;

import java.math.BigDecimal;
import java.util.List;

public record PropertyFilterCriteria(
        String q,
        String city,
        String locality,
        BigDecimal minRent,
        BigDecimal maxRent,
        List<PropertyConfig> configs,
        List<Furnishing> furnishings,
        PreferredTenants preferredTenants,
        Boolean availableImmediately,
        String sort, // "newest", "rentAsc", "rentDesc"
        Integer page,
        Integer size
) {
    public int getPageOrDefault() {
        return (page != null && page >= 0) ? page : 0;
    }

    public int getSizeOrDefault() {
        return (size != null && size > 0 && size <= 100) ? size : 20;
    }
}

