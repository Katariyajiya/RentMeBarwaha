package com.project.rentmebarwaha.property.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.property.service.PropertyBrowseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/localities")
@RequiredArgsConstructor
@Tag(name = "Localities (Public)", description = "Location selector and search suggestions for city localities")
public class LocalityController {

    private final PropertyBrowseService propertyBrowseService;

    @GetMapping
    @Operation(summary = "Get Localities", description = "Returns distinct localities in a city having active listings.")
    public ResponseEntity<ApiResponse<List<String>>> getLocalities(
            @RequestParam(required = false, defaultValue = "Barwaha") String city) {
        List<String> localities = propertyBrowseService.getLocalities(city);
        return ResponseEntity.ok(ApiResponse.ok(localities));
    }
}

