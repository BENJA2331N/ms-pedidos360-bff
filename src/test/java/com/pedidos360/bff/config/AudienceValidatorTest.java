package com.pedidos360.bff.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AudienceValidatorTest {

    @Test
    @DisplayName("Debe validar exitosamente cuando el claim 'aud' coincide con la audiencia esperada")
    void shouldPassWhenAudienceMatches() {
        String expectedAudience = "api://pedidos360-bff";
        AudienceValidator validator = new AudienceValidator(expectedAudience);

        Jwt jwt = new Jwt(
                "token-sample",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("aud", List.of("api://pedidos360-bff"))
        );

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Debe fallar cuando el claim 'aud' no coincide con la audiencia configurada")
    void shouldFailWhenAudienceDoesNotMatch() {
        String expectedAudience = "api://pedidos360-bff";
        AudienceValidator validator = new AudienceValidator(expectedAudience);

        Jwt jwt = new Jwt(
                "token-sample",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("aud", List.of("api://otra-audiencia"))
        );

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
        assertEquals("invalid_token", result.getErrors().iterator().next().getErrorCode());
    }

    @Test
    @DisplayName("Debe pasar cuando la audiencia es placeholder (modo desarrollo flexible)")
    void shouldPassWhenAudienceIsPlaceholder() {
        AudienceValidator validator = new AudienceValidator("api://<API_CLIENT_ID>");

        Jwt jwt = new Jwt(
                "token-sample",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("aud", List.of("cualquiera"))
        );

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertFalse(result.hasErrors());
    }
}
