package com.aarw.fitdata.oauth;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
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
    public ResponseEntity<Void> start(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Referer", required = false) String referer,
            HttpSession session
    ) {
        if (props.clientId() == null || props.clientId().isBlank()) {
            return ResponseEntity.status(500).build();
        }

        if (referer != null && !referer.contains("/oauth/fitbit/start")) {
            session.setAttribute("oauth_origin", referer);
        }

        String state = UUID.randomUUID().toString();
        String verifier = Pkce.verifier();
        String challenge = Pkce.challengeS256(verifier);

        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_VERIFIER, verifier);

        String scopeParam = props.scope() == null ? "" : props.scope().trim();
        if (scopeParam.contains(" ")) {
            scopeParam = scopeParam.replace(" ", "+");
        }

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

    @GetMapping("/oauth/fitbit/status")
    public ResponseEntity<AuthStatusResponse> status(HttpSession session) {
        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        boolean authenticated = context != null && context.getAuthentication() != null && context.getAuthentication().isAuthenticated();
        return ResponseEntity.ok(new AuthStatusResponse(authenticated));
    }

    @GetMapping("/oauth/fitbit/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context != null && context.getAuthentication() != null) {
            String userId = context.getAuthentication().getName();
            tokenRepository.findByFitbitUserId(userId).ifPresent(tokenRepository::delete);
        }
        session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/oauth/fitbit/callback")
    public ResponseEntity<Void> callback(
            @org.springframework.web.bind.annotation.RequestParam String code,
            @org.springframework.web.bind.annotation.RequestParam String state,
            HttpSession session
    ) {
        log.info("Callback received with state={}", state);
        String savedState = (String) session.getAttribute(SESSION_STATE);
        String verifier = (String) session.getAttribute(SESSION_VERIFIER);

        if (savedState == null || !savedState.equals(state) || verifier == null) {
            log.error("OAuth callback state mismatch or missing verifier. savedState={}, receivedState={}", savedState, state);
            return ResponseEntity.status(400).build();
        }

        try {
            FitbitTokenResponse resp = exchangeCodeForToken(code, verifier);

            FitbitTokenEntity entity = tokenRepository.findByFitbitUserId(resp.userId())
                    .orElse(new FitbitTokenEntity());

            entity.setFitbitUserId(resp.userId());
            entity.setAccessToken(resp.accessToken());
            entity.setRefreshToken(resp.refreshToken());
            entity.setTokenType(resp.tokenType());
            entity.setScope(resp.scope());
            entity.setExpiresAt(resp.expiresAt());

            tokenRepository.save(entity);

            // Establish SecurityContext for the session
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    resp.userId(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            log.info("Fitbit OAuth successful for user={}", resp.userId());
            
            // Redirect to frontend
            String target = "/";
            if (session.getAttribute("oauth_origin") != null) {
                target = (String) session.getAttribute("oauth_origin");
                // Remove potential hash fragments like #_=_ added by some providers
                if (target.contains("#")) {
                    target = target.substring(0, target.indexOf("#"));
                }
            }
            
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, target).build();
        } catch (Exception e) {
            log.error("OAuth exchange failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    public record AuthStatusResponse(boolean authenticated) {}

    private FitbitTokenResponse exchangeCodeForToken(String code, String verifier) {
        log.info("Exchanging code for token, redirect_uri={}", props.redirectUri());
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
                .bodyToMono(new ParameterizedTypeReference<java.util.Map<String, Object>>() {})
                .block();

        FitbitTokenResponse resp = FitbitTokenResponse.fromMap(token);
        if (resp == null) {
            throw new IllegalStateException("Token response is null");
        }

        return resp;
    }
}