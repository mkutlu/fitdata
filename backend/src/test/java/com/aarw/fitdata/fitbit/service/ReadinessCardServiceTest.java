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

class ReadinessCardServiceTest {

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

    @Test
    void testGetReadinessCard_EstimatesSuccessfully() {
        LocalDate today = LocalDate.now();

        // API returns VO2 Max
        when(apiClient.getVo2Max(any(), eq(today.toString())))
                .thenReturn(new FitbitVo2MaxResponse(List.of(
                        new FitbitVo2MaxResponse.CardioScore(today.toString(), new FitbitVo2MaxResponse.Value("45-49"))
                )));

        // Mock dependencies for estimation
        when(heartRateService.getDay(today)).thenReturn(new HeartRateDayDto(today, 60, new HeartRateDayDto.Zones(0, 0, 0, 0)));
        when(heartRateService.getRange(any(), eq(today))).thenReturn(new HeartRateRangeDto("LAST_7_DAYS", today.minusDays(7), today, List.of(
                new HeartRateRangeDto.Point(today, 60, new HeartRateDayDto.Zones(0, 0, 0, 0))
        )));
        when(sleepService.getSleep(today)).thenReturn(new SleepDto(today.toString(), 480, 500, 85, "22:00", "06:00", new SleepDto.LevelsSummary(0, 0, 0, 0), List.of()));
        when(apiClient.getActivitiesSummaryForDay(any(), eq(today.toString()))).thenReturn(new FitbitActivitiesSummaryResponse(new FitbitActivitiesSummaryResponse.Summary(2000, 1000)));

        // Mock HRV responses
        when(apiClient.getHrv(any(), eq(today.toString()))).thenReturn(new FitbitHrvResponse(List.of(
                new FitbitHrvResponse.Hrv(new FitbitHrvResponse.Value(50.0, 55.0), today.toString())
        )));
        when(apiClient.getHrvRange(any(), any(), any())).thenReturn(new FitbitHrvResponse(List.of(
                new FitbitHrvResponse.Hrv(new FitbitHrvResponse.Value(45.0, 50.0), today.minusDays(1).toString())
        )));

        ReadinessCardDto result = service.getReadinessCard(today);

        assertNotNull(result);
        assertEquals(today, result.date());
        assertNotNull(result.readinessScore());
        assertTrue(result.readinessScore() > 0);
        assertEquals("ESTIMATED", result.readinessStatus());
        assertEquals(45, result.cardioLoadScore()); // Parsed from "45-49"
        assertEquals("45-49", result.vo2Max());
    }

    @Test
    void testGetReadinessCard_DoesNotCallNonExistentEndpoints() {
        LocalDate today = LocalDate.now();
        
        // Mock data for VO2 Max
        when(apiClient.getVo2Max(any(), eq(today.toString())))
                .thenReturn(new FitbitVo2MaxResponse(Collections.emptyList()));

        // Mock estimation dependencies to avoid NullPointerException
        when(heartRateService.getDay(today)).thenReturn(new HeartRateDayDto(today, 60, new HeartRateDayDto.Zones(0, 0, 0, 0)));
        when(heartRateService.getRange(any(), eq(today))).thenReturn(new HeartRateRangeDto("LAST_7_DAYS", today.minusDays(7), today, List.of()));
        when(sleepService.getSleep(today)).thenReturn(new SleepDto(today.toString(), 0, 0, null, null, null, null, List.of()));

        service.getReadinessCard(today);

        // Verify official endpoints are called
        verify(apiClient, times(1)).getVo2Max(any(), eq(today.toString()));
    }
}
