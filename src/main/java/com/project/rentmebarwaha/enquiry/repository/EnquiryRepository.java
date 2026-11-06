package com.project.rentmebarwaha.enquiry.repository;

import com.project.rentmebarwaha.enquiry.entity.Enquiry;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    Optional<Enquiry> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByTenantIdAndPropertyIdAndCreatedAtAfter(UUID tenantId, UUID propertyId, Instant after);

    Page<Enquiry> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.deletedAt IS NULL")
    Page<Enquiry> findByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.status = :status AND e.deletedAt IS NULL")
    Page<Enquiry> findByOwnerIdAndStatus(@Param("ownerId") UUID ownerId, @Param("status") EnquiryStatus status, Pageable pageable);

    @Query("SELECT e FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.property.id = :propertyId AND e.deletedAt IS NULL")
    Page<Enquiry> findByOwnerIdAndPropertyId(@Param("ownerId") UUID ownerId, @Param("propertyId") UUID propertyId, Pageable pageable);

    @Query("SELECT e FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.property.id = :propertyId AND e.status = :status AND e.deletedAt IS NULL")
    Page<Enquiry> findByOwnerIdAndPropertyIdAndStatus(@Param("ownerId") UUID ownerId, @Param("propertyId") UUID propertyId, @Param("status") EnquiryStatus status, Pageable pageable);

    List<Enquiry> findByPropertyIdAndStatusInAndIdNot(UUID propertyId, List<EnquiryStatus> statuses, UUID excludeId);

    List<Enquiry> findByPropertyIdAndStatusIn(UUID propertyId, List<EnquiryStatus> statuses);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.status = :status AND e.deletedAt IS NULL")
    long countByOwnerIdAndStatus(@Param("ownerId") UUID ownerId, @Param("status") EnquiryStatus status);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.deletedAt IS NULL")
    long countByOwnerId(@Param("ownerId") UUID ownerId);

    long countByPropertyIdAndDeletedAtIsNull(UUID propertyId);

    long countByPropertyIdAndStatusAndDeletedAtIsNull(UUID propertyId, EnquiryStatus status);

    @Query("SELECT e FROM Enquiry e WHERE e.property.owner.id = :ownerId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<Enquiry> findRecentByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);
}

