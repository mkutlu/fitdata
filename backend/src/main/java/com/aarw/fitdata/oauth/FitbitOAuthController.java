package com.aarw.fitdata.oauth;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@RestController
public class FitbitOAuthController {

    private static final Logger log = LoggerFactory.getLogger(FitbitOAuthController.class);

    private static final String SESSION_STATE = "fitbit_oauth_state";
    private static final String SESSION_VERIFIER = "fitbit_pkce_verifier";

    private final FitbitProps props;
    private final WebClient webClient;
    private final FitbitTokenRepository tokenRepository;

    public FitbitOAuthController(FitbitProps props, WebClient.Builder builder, FitbitTokenRepository tokenRepository) {
        this.props = props;
        this.webClient = builder.build();
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/oauth/fitbit/start")
    public ResponseEntity<Void> start(HttpSession session) {
        if (props.clientId() == null || props.clientId().isBlank()) {
            return ResponseEntity.status(500).build();
        }

        String state = UUID.randomUUID().toString();
        String verifier = Pkce.verifier();
        String challenge = Pkce.challengeS256(verifier);

        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_VERIFIER, verifier);

        String scopeParam = props.scope() == null ? "" : props.scope().trim().replace(" ", "+");

        String url = UriComponentsBuilder
                .fromUriString(props.authorizeUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("scope", scopeParam)
                .queryParam("state", state)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .build(true)
                .toUriString();

        log.info("Fitbit OAuth start redirect prepared, state={}", state);
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, url).build();
    }

    @GetMapping("/oauth/fitbit/callback")
    public ResponseEntity<OAuthConnectResponse> callback(
            String code,
            String state,
            String error,
            HttpSession session
    ) {
        if (error != null && !error.isBlank()) {
            log.warn("Fitbit OAuth error={}", error);
            return ResponseEntity
                    .badRequest()
                    .body(new OAuthConnectResponse(false, error));
        }

        String expectedState = (String) session.getAttribute(SESSION_STATE);
        String verifier = (String) session.getAttribute(SESSION_VERIFIER);

        if (expectedState == null || verifier == null) {
            log.warn("OAuth session missing state/verifier");
            return ResponseEntity.badRequest().body(new OAuthConnectResponse(false, "session_missing"));
        }
        if (!expectedState.equals(state)) {
            log.warn("State mismatch expected={}, got={}", expectedState, state);
            return ResponseEntity.badRequest().body(new OAuthConnectResponse(false, "state_mismatch"));
        }
        if (code == null || code.isBlank()) {
            log.warn("Missing authorization code");
            return ResponseEntity.badRequest().body(new OAuthConnectResponse(false, "code_missing"));
        }

        session.removeAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_VERIFIER);

        FitbitTokenResponse token = exchangeCodeForToken(code, verifier);

        FitbitTokenEntity entity = tokenRepository.findByFitbitUserId(token.userId())
                .orElseGet(FitbitTokenEntity::new);

        entity.setFitbitUserId(token.userId());
        entity.setAccessToken(token.accessToken());
        entity.setRefreshToken(token.refreshToken());
        entity.setTokenType(token.tokenType());
        entity.setScope(token.scope());
        entity.setExpiresAt(token.expiresAt());

        tokenRepository.save(entity);

        log.info("Saved Fitbit token for user_id={} expires_at={}", token.userId(), entity.getExpiresAt());

        return ResponseEntity.ok(new OAuthConnectResponse(true, "Fitbit connected successfully"));

    }

    private FitbitTokenResponse exchangeCodeForToken(String code, String verifier) {
        String auth = Base64.getEncoder().encodeToString(
                (props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", props.clientId());
        form.add("redirect_uri", props.redirectUri());
        form.add("code", code);
        form.add("code_verifier", verifier);

        var token = webClient.post()
                .uri(props.tokenUri())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();

        if (token == null) {
            throw new IllegalStateException("Token response is null");
        }

        String accessToken = String.valueOf(token.get("access_token"));
        String refreshToken = String.valueOf(token.get("refresh_token"));
        String tokenType = token.get("token_type") == null ? null : String.valueOf(token.get("token_type"));
        String scope = token.get("scope") == null ? null : String.valueOf(token.get("scope"));
        String userId = token.get("user_id") == null ? null : String.valueOf(token.get("user_id"));
        long expiresIn = token.get("expires_in") == null ? 0L : Long.parseLong(String.valueOf(token.get("expires_in")));

        return new FitbitTokenResponse(accessToken, refreshToken, tokenType, scope, userId, expiresIn);
    }
}