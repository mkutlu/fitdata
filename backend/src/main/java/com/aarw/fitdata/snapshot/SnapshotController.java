package com.aarw.fitdata.snapshot;

import com.aarw.fitdata.dto.DashboardSnapshotDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.service.*;
import com.aarw.fitdata.fitbit.util.StepsRange;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
@Slf4j
public class SnapshotController {

    private final SnapshotRepository snapshotRepository;
    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;
    private final ReadinessCardService readinessCardService;
    private final StepsService stepsService;
    private final WeightService weightService;
    private final HeartRateIntradayService heartRateIntradayService;
    private final SleepService sleepService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<UUID> createSnapshot(
            @RequestParam LocalDate selectedDate,
            @RequestParam(defaultValue = "LAST_7_DAYS") StepsRange stepsRange,
            @RequestParam(defaultValue = "LAST_7_DAYS") StepsRange weightRange
    ) {
        log.info("Creating snapshot for date={}, stepsRange={}, weightRange={}", selectedDate, stepsRange, weightRange);
        
        try {
            FitbitTokenEntity token = tokenService.getValidTokenOrThrow();
            
            DashboardSnapshotDto data = new DashboardSnapshotDto(
                    selectedDate,
                    stepsRange,
                    weightRange,
                    apiClient.getProfile(token),
                    readinessCardService.getReadinessCard(selectedDate),
                    stepsService.getSteps(stepsRange, selectedDate),
                    weightService.getWeight(weightRange, selectedDate),
                    heartRateIntradayService.get(selectedDate),
                    sleepService.getSleep(selectedDate)
            );

            String jsonData = objectMapper.writeValueAsString(data);
            
            Snapshot snapshot = new Snapshot();
            snapshot.setId(UUID.randomUUID());
            snapshot.setData(jsonData);
            
            snapshotRepository.save(snapshot);
            
            return ResponseEntity.ok(snapshot.getId());
        } catch (Exception e) {
            log.error("Failed to create snapshot", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DashboardSnapshotDto> getSnapshot(@PathVariable UUID id) {
        return snapshotRepository.findById(id)
                .map(snapshot -> {
                    try {
                        return ResponseEntity.ok(objectMapper.readValue(snapshot.getData(), DashboardSnapshotDto.class));
                    } catch (Exception e) {
                        log.error("Failed to deserialize snapshot data", e);
                        return ResponseEntity.internalServerError().<DashboardSnapshotDto>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
