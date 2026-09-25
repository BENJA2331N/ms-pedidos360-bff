package com.pedidos360.bff.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validador personalizado para asegurar que el JWT contenga la audiencia requerida (claim 'aud').
 * Por defecto, Spring Security con issuer-uri no valida el audience automǭticamente.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String audience;

    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (this.audience == null || this.audience.isBlank() || this.audience.contains("<API_CLIENT_ID>")) {
            return OAuth2TokenValidatorResult.success();
        }
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.contains(this.audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token no contiene el audience requerido ('aud'): " + this.audience,
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }

    public String getAudience() {
        return audience;
    }
}
