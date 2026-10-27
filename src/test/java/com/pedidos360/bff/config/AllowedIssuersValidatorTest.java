package com.pedidos360.bff.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AllowedIssuersValidatorTest {

    private static final String TENANT = "c8d0fa9a-8d78-47b0-b443-f5419b74a5a8";
    private static final String ISSUER_V2 = "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    private static final String ISSUER_V1 = "https://sts.windows.net/" + TENANT + "/";

    private final AllowedIssuersValidator validator =
            new AllowedIssuersValidator(AllowedIssuersValidator.allowedFor(ISSUER_V2));

    private Jwt jwtWithIssuer(String iss) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "none"), Map.of("iss", iss));
    }

    @Test
    @DisplayName("Deriva el issuer v2 configurado y el issuer v1 del mismo tenant")
    void allowedForDerivesV1AndV2() {
        Set<String> allowed = AllowedIssuersValidator.allowedFor(ISSUER_V2);
        assertEquals(Set.of(ISSUER_V2, ISSUER_V1), allowed);
    }

    @Test
    @DisplayName("Acepta tokens con issuer v2 del tenant configurado")
    void acceptsV2() {
        assertFalse(validator.validate(jwtWithIssuer(ISSUER_V2)).hasErrors());
    }

    @Test
    @DisplayName("Acepta tokens con issuer v1 (sts.windows.net) del mismo tenant")
    void acceptsV1() {
        assertFalse(validator.validate(jwtWithIssuer(ISSUER_V1)).hasErrors());
    }

    @Test
    @DisplayName("Rechaza tokens emitidos para OTRO tenant (aunque el host sea de Microsoft)")
    void rejectsOtherTenant() {
        OAuth2TokenValidatorResult r1 = validator.validate(
                jwtWithIssuer("https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000/v2.0"));
        OAuth2TokenValidatorResult r2 = validator.validate(
                jwtWithIssuer("https://sts.windows.net/00000000-0000-0000-0000-000000000000/"));
        assertTrue(r1.hasErrors());
        assertTrue(r2.hasErrors());
    }

    @Test
    @DisplayName("Rechaza tokens de un emisor arbitrario")
    void rejectsArbitraryIssuer() {
        assertTrue(validator.validate(jwtWithIssuer("https://evil.example.com/" + TENANT + "/")).hasErrors());
    }

    @Test
    @DisplayName("Placeholder o vacío no produce emisores permitidos")
    void placeholderYieldsEmptySet() {
        assertTrue(AllowedIssuersValidator.allowedFor(null).isEmpty());
        assertTrue(AllowedIssuersValidator.allowedFor(" ").isEmpty());
        assertTrue(AllowedIssuersValidator.allowedFor("https://login.microsoftonline.com/<TENANT_ID>/v2.0").isEmpty());
    }
}
