package com.aarw.fitdata.oauth;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.oauth.token.FitbitTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FitbitOAuthControllerTest {

    private FitbitOAuthController controller;

    @BeforeEach
    void setUp() {
        FitbitProps props = new FitbitProps(
                "http://api", "client", "secret", "http://redirect", 
                "http://auth", "http://token", "scope"
        );
        WebClient.Builder builder = mock(WebClient.Builder.class);
        FitbitTokenRepository repo = mock(FitbitTokenRepository.class);
        controller = new FitbitOAuthController(props, builder, repo);
        SecurityContextHolder.clearContext();
    }

    @Test
    void status_ReturnsAuthenticatedFalse_WhenNoAuth() {
        ResponseEntity<FitbitOAuthController.AuthStatusResponse> response = controller.status();
        
        assertNotNull(response.getBody());
        assertFalse(response.getBody().authenticated());
    }

    @Test
    void status_ReturnsAuthenticatedTrue_WhenAuthPresent() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user123", null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        ResponseEntity<FitbitOAuthController.AuthStatusResponse> response = controller.status();
        
        assertNotNull(response.getBody());
        assertTrue(response.getBody().authenticated());
    }

    @Test
    void status_ReturnsAuthenticatedFalse_WhenAnonymous() {
        new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of()
        );
        // Note: In real Spring Security it would be AnonymousAuthenticationToken, 
        // but our implementation checks the name or class.
        // Let's test with actual AnonymousAuthenticationToken if possible or just mock it.
        
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            )
        );

        ResponseEntity<FitbitOAuthController.AuthStatusResponse> response = controller.status();
        
        assertNotNull(response.getBody());
        assertFalse(response.getBody().authenticated());
    }
}
