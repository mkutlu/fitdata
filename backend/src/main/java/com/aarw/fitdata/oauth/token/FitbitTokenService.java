package com.aarw.fitdata.oauth.token;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.oauth.FitbitTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        FitbitTokenEntity token = repo.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Fitbit token found. Connect Fitbit first."));

        if (token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return token;
        }

        log.info("Fitbit access token expired or near expiry, refreshing");
        FitbitTokenResponse refreshed = refresh(token.getRefreshToken());

        token.setAccessToken(refreshed.accessToken());
        token.setRefreshToken(refreshed.refreshToken());
        token.setTokenType(refreshed.tokenType());
        token.setScope(refreshed.scope());
        token.setExpiresAt(refreshed.expiresAt());

        return repo.save(token);
    }

    private FitbitTokenResponse refresh(String refreshToken) {
        String auth = Base64.getEncoder().encodeToString(
                (props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        Map body = webClient.post()
                .uri(props.tokenUri())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (body == null) {
            throw new IllegalStateException("Refresh token response is null");
        }

        String access = String.valueOf(body.get("access_token"));
        String refresh = String.valueOf(body.get("refresh_token"));
        String type = body.get("token_type") == null ? null : String.valueOf(body.get("token_type"));
        String scope = body.get("scope") == null ? null : String.valueOf(body.get("scope"));
        String userId = body.get("user_id") == null ? null : String.valueOf(body.get("user_id"));
        long expiresIn = body.get("expires_in") == null ? 0L : Long.parseLong(String.valueOf(body.get("expires_in")));

        return new FitbitTokenResponse(access, refresh, type, scope, userId, expiresIn);
    }
}
