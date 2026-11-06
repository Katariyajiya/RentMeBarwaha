package com.project.rentmebarwaha.property.repository;

import com.project.rentmebarwaha.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

    List<PropertyImage> findByPropertyIdOrderBySortOrderAsc(UUID propertyId);

    Optional<PropertyImage> findFirstByPropertyIdAndIsCoverTrue(UUID propertyId);

    Optional<PropertyImage> findByIdAndPropertyId(UUID id, UUID propertyId);

    void deleteByIdAndPropertyId(UUID id, UUID propertyId);

    long countByPropertyId(UUID propertyId);
}

