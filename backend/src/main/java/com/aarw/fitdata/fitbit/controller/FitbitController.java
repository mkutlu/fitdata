package com.aarw.fitdata.fitbit.controller;

import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitProfileResponse;
import com.aarw.fitdata.dto.StepsSeriesDto;
import com.aarw.fitdata.fitbit.service.StepsService;
import com.aarw.fitdata.fitbit.util.StepsRange;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FitbitController {

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;
    private final StepsService stepsService;


    public FitbitController(FitbitTokenService tokenService, FitbitApiClient apiClient, StepsService stepsService) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
        this.stepsService = stepsService;
    }

    @GetMapping("/api/profile")
    public FitbitProfileResponse profile() {
        FitbitTokenEntity token = tokenService.getValidTokenOrThrow();
        return apiClient.getProfile(token);
    }

    @GetMapping("/api/steps")
    public StepsSeriesDto steps(@RequestParam(defaultValue = "LAST_7_DAYS") StepsRange range) {
        return stepsService.getSteps(range);
    }
}