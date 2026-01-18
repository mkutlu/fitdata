package com.aarw.fitdata.oauth.token;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.oauth.FitbitTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class FitbitTokenService {

    private static final Logger log = LoggerFactory.getLogger(FitbitTokenService.class);

    private final FitbitProps props;
    private final FitbitTokenRepository repo;
    private final WebClient webClient;

    public FitbitTokenService(FitbitProps props, FitbitTokenRepository repo, WebClient.Builder builder) {
        this.props = props;
        this.repo = repo;
        this.webClient = builder.build();
    }

    public FitbitTokenEntity getValidTokenOrThrow() {
        String userId = SecurityContextHolder.getContext().getAuthentication() != null 
                ? SecurityContextHolder.getContext().getAuthentication().getName() 
                : null;

        FitbitTokenEntity token;
        if (userId != null && !"anonymousUser".equals(userId)) {
            token = repo.findByFitbitUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("No Fitbit token found for user: " + userId));
        } else {
            // Fallback for non-authenticated (should be blocked by SecurityConfig, but for safety)
            token = repo.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Fitbit token found. Connect Fitbit first."));
        }

        if (token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return token;
        }

        log.info("Fitbit access token expired or near expiry, refreshing");
        try {
            FitbitTokenResponse refreshed = refresh(token.getRefreshToken());

            token.setAccessToken(refreshed.accessToken());
            token.setRefreshToken(refreshed.refreshToken());
            token.setTokenType(refreshed.tokenType());
            token.setScope(refreshed.scope());
            token.setExpiresAt(refreshed.expiresAt());

            return repo.save(token);
        } catch (Exception e) {
            log.warn("Failed to refresh Fitbit token for user {}: {}", token.getFitbitUserId(), e.getMessage());
            // We don't delete the token here immediately because a temporary network issue 
            // shouldn't destroy the connection. Let the user retry or handle it in the controller.
            throw new IllegalStateException("Fitbit connection issue. Please try again or login.", e);
        }
    }

    private FitbitTokenResponse refresh(String refreshToken) {
        String auth = Base64.getEncoder().encodeToString(
                (props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        Map<String, Object> body = webClient.post()
                .uri(props.tokenUri())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        FitbitTokenResponse refreshed = FitbitTokenResponse.fromMap(body);
        if (refreshed == null) {
            throw new IllegalStateException("Refresh token response is null");
        }

        return refreshed;
    }
}
