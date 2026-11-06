package com.project.rentmebarwaha.visit.service;

import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.visit.dto.ScheduleVisitRequest;
import com.project.rentmebarwaha.visit.dto.UpdateVisitStatusRequest;
import com.project.rentmebarwaha.visit.dto.VisitResponse;

import java.util.List;
import java.util.UUID;

public interface VisitService {

    VisitResponse scheduleVisit(UUID userId, UUID enquiryId, ScheduleVisitRequest request);

    PageResponse<VisitResponse> getMyVisits(UUID userId, int page, int size);

    List<VisitResponse> getUpcomingVisitsForOwner(UUID ownerId, int limit);

    VisitResponse getVisitById(UUID userId, UUID visitId);

    VisitResponse updateVisitStatus(UUID userId, UUID visitId, UpdateVisitStatusRequest request);
}

