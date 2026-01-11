package com.aarw.fitdata.fitbit.controller;

import com.aarw.fitdata.dto.HeartRateDayDto;
import com.aarw.fitdata.dto.HeartRateIntradayDto;
import com.aarw.fitdata.dto.HeartRateRangeDto;
import com.aarw.fitdata.fitbit.service.HeartRateIntradayService;
import com.aarw.fitdata.fitbit.service.HeartRateService;
import com.aarw.fitdata.fitbit.util.StepsRange;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class HeartRateController {

    private final HeartRateService heartRateService;
    private final HeartRateIntradayService heartRateIntradayService;

    public HeartRateController(HeartRateService heartRateService, HeartRateIntradayService heartRateIntradayService) {
        this.heartRateService = heartRateService;
        this.heartRateIntradayService = heartRateIntradayService;
    }

    @GetMapping("/api/heartrate")
    public HeartRateDayDto day(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        LocalDate effective = baseDate == null ? LocalDate.now() : baseDate;
        return heartRateService.getDay(effective);
    }

    @GetMapping("/api/heartrate/range")
    public HeartRateRangeDto range(
            @RequestParam StepsRange range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        LocalDate effective = baseDate == null ? LocalDate.now() : baseDate;
        return heartRateService.getRange(range, effective);
    }

    @GetMapping("/api/heartrate/intraday")
    public HeartRateIntradayDto intraday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate
    ) {
        LocalDate effective = baseDate == null ? LocalDate.now() : baseDate;
        return heartRateIntradayService.get(effective);
    }
}