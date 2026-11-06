package com.project.rentmebarwaha.rental.repository;

import com.project.rentmebarwaha.rental.entity.RentalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalRecordRepository extends JpaRepository<RentalRecord, UUID> {

    Optional<RentalRecord> findFirstByPropertyIdOrderByCreatedAtDesc(UUID propertyId);
}

