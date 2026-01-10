package com.aarw.fitdata.fitbit.controller;

import com.aarw.fitdata.fitbit.dto.FitbitProfileResponse;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FitbitController {

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;

    public FitbitController(FitbitTokenService tokenService, FitbitApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
    }

    @GetMapping("/api/fitbit/profile")
    public FitbitProfileResponse profile() {
        FitbitTokenEntity token = tokenService.getValidTokenOrThrow();
        return apiClient.getProfile(token);
    }
}