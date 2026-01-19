package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.HeartRateDayDto;
import com.aarw.fitdata.dto.HeartRateRangeDto;
import com.aarw.fitdata.dto.ReadinessCardDto;
import com.aarw.fitdata.dto.SleepDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitActivitiesSummaryResponse;
import com.aarw.fitdata.fitbit.dto.FitbitHrvResponse;
import com.aarw.fitdata.fitbit.dto.FitbitVo2MaxResponse;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReadinessReproductionTest {

    private FitbitApiClient apiClient;
    private HeartRateService heartRateService;
    private SleepService sleepService;
    private ReadinessCardService service;

    @BeforeEach
    void setUp() {
        FitbitTokenService tokenService = mock(FitbitTokenService.class);
        apiClient = mock(FitbitApiClient.class);
        heartRateService = mock(HeartRateService.class);
        sleepService = mock(SleepService.class);
        service = new ReadinessCardService(tokenService, apiClient, heartRateService, sleepService);

        when(tokenService.getValidTokenOrThrow()).thenReturn(new FitbitTokenEntity());
    }

    /**
     * Reproduces the issue where readiness score would return null (interpreted as 0 by frontend)
     * if RHR data was missing for both today and the last 7 days.
     */
    @Test
    void testReadinessEstimation_WithMissingRhr_ReturnsValidScore() {
        LocalDate today = LocalDate.now();

        // RHR is null for today
        when(heartRateService.getDay(today)).thenReturn(new HeartRateDayDto(today, null, new HeartRateDayDto.Zones(0, 0, 0, 0)));
        // RHR is empty for the last 7 days
        when(heartRateService.getRange(any(), eq(today))).thenReturn(new HeartRateRangeDto("LAST_7_DAYS", today.minusDays(7), today, List.of()));

        // VO2 Max returns empty
        when(apiClient.getVo2Max(any(), eq(today.toString()))).thenReturn(new FitbitVo2MaxResponse(Collections.emptyList()));
        // Activities
        when(apiClient.getActivitiesSummaryForDay(any(), eq(today.toString()))).thenReturn(new FitbitActivitiesSummaryResponse(new FitbitActivitiesSummaryResponse.Summary(0, 0)));
        // Sleep
        when(sleepService.getSleep(today)).thenReturn(new SleepDto(today.toString(), 0, 0, null, null, null, null, List.of()));
        // HRV
        when(apiClient.getHrv(any(), eq(today.toString()))).thenReturn(new FitbitHrvResponse(List.of()));
        when(apiClient.getHrvRange(any(), any(), any())).thenReturn(new FitbitHrvResponse(List.of()));

        // Act
        ReadinessCardDto result = service.getReadinessCard(today);

        // Assert
        assertNotNull(result, "ReadinessCardDto should not be null");
        assertNotNull(result.readinessScore(), "Readiness score should not be null even if RHR is missing");
        assertTrue(result.readinessScore() > 0, "Readiness score should be a positive number (neutral calculation)");
        assertEquals("ESTIMATED", result.readinessStatus());
    }

    @Test
    void testReadinessEstimation_WithApiException_ReturnsValidScore() {
        LocalDate today = LocalDate.now();

        // Simulate an exception in one of the services
        when(heartRateService.getDay(today)).thenThrow(new RuntimeException("API Down"));
        
        // Other services return valid data
        when(heartRateService.getRange(any(), eq(today))).thenReturn(new HeartRateRangeDto("LAST_7_DAYS", today.minusDays(7), today, List.of()));
        when(apiClient.getVo2Max(any(), eq(today.toString()))).thenReturn(new FitbitVo2MaxResponse(Collections.emptyList()));
        when(apiClient.getActivitiesSummaryForDay(any(), eq(today.toString()))).thenReturn(new FitbitActivitiesSummaryResponse(new FitbitActivitiesSummaryResponse.Summary(0, 0)));
        when(sleepService.getSleep(today)).thenReturn(new SleepDto(today.toString(), 0, 0, null, null, null, null, List.of()));
        when(apiClient.getHrv(any(), eq(today.toString()))).thenReturn(new FitbitHrvResponse(List.of()));
        when(apiClient.getHrvRange(any(), any(), any())).thenReturn(new FitbitHrvResponse(List.of()));

        // Act
        ReadinessCardDto result = service.getReadinessCard(today);

        // Assert
        assertNotNull(result);
        assertNotNull(result.readinessScore(), "Readiness score should not be null even if an API call fails");
        assertTrue(result.readinessScore() > 0, "Readiness score should be a positive number (neutral fallback)");
    }
}
