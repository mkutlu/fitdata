package com.aarw.fitdata.fitbit.controller;

import com.aarw.fitdata.dto.ReadinessCardDto;
import com.aarw.fitdata.fitbit.service.ReadinessCardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class ReadinessController {

    private final ReadinessCardService readinessCardService;

    public ReadinessController(ReadinessCardService readinessCardService) {
        this.readinessCardService = readinessCardService;
    }

    @GetMapping("/api/readiness")
    public ReadinessCardDto getReadinessCard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        LocalDate effective = baseDate == null ? LocalDate.now() : baseDate;
        return readinessCardService.getReadinessCard(effective);
    }
}
