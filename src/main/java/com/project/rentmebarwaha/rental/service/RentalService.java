package com.project.rentmebarwaha.rental.service;

import com.project.rentmebarwaha.rental.dto.MarkRentedRequest;
import com.project.rentmebarwaha.rental.dto.RentalRecordResponse;

import java.util.UUID;

public interface RentalService {

    RentalRecordResponse markPropertyAsRented(UUID ownerId, UUID propertyId, MarkRentedRequest request);

    RentalRecordResponse getRentalRecordByProperty(UUID ownerId, UUID propertyId);
}

