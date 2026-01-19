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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReadinessController.class);

    private final ReadinessCardService readinessCardService;

    public ReadinessController(ReadinessCardService readinessCardService) {
        this.readinessCardService = readinessCardService;
    }

    @GetMapping("/api/readiness")
    public ReadinessCardDto getReadinessCard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        log.info("Readiness request START");
        LocalDate effective = baseDate == null ? LocalDate.now() : baseDate;
        var result = readinessCardService.getReadinessCard(effective);
        log.info("Readiness request END");
        return result;
    }
}
