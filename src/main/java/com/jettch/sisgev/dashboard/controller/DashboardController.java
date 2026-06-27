package com.jettch.sisgev.dashboard.controller;

import com.jettch.sisgev.dashboard.dto.KmByConditionResponse;
import com.jettch.sisgev.dashboard.dto.MapSegmentResponse;
import com.jettch.sisgev.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/km-by-condition")
    public KmByConditionResponse kmByCondition(@RequestParam(required = false) UUID municipalityId) {
        return service.kmByCondition(municipalityId);
    }

    @GetMapping("/summary")
    public KmByConditionResponse summary(@RequestParam(required = false) UUID municipalityId) {
        return service.kmByCondition(municipalityId);
    }

    @GetMapping("/map-segments")
    public List<MapSegmentResponse> mapSegments(@RequestParam(required = false) UUID municipalityId) {
        return service.mapSegments(municipalityId);
    }
}
