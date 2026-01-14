package com.aarw.fitdata.fitbit.controller;

import com.aarw.fitdata.dto.WeightSeriesDto;
import com.aarw.fitdata.fitbit.service.WeightService;
import com.aarw.fitdata.fitbit.util.StepsRange;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class WeightController {

    private final WeightService weightService;

    public WeightController(WeightService weightService) {
        this.weightService = weightService;
    }

    @GetMapping("/api/weight/range")
    public WeightSeriesDto range(
            @RequestParam StepsRange range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        LocalDate effective = baseDate == null ? LocalDate.now() : baseDate;
        return weightService.getWeight(range, effective);
    }
}
