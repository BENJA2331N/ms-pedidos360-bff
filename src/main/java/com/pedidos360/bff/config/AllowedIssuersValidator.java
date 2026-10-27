package com.pedidos360.bff.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Valida que el claim 'iss' del JWT sea EXACTAMENTE uno de los emisores permitidos.
 * <p>
 * Microsoft Entra ID emite tokens con issuer v2 ({@code https://login.microsoftonline.com/{tenant}/v2.0})
 * o v1 ({@code https://sts.windows.net/{tenant}/}) según la versión de token de la API registrada.
 * Se aceptan ambos formatos pero ÚNICAMENTE para el tenant configurado, de modo que un token válido
 * de otro tenant sea rechazado.
 */
public class AllowedIssuersValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> allowedIssuers;

    public AllowedIssuersValidator(Collection<String> allowedIssuers) {
        this.allowedIssuers = new LinkedHashSet<>(allowedIssuers);
    }

    /**
     * Deriva los emisores permitidos a partir del issuer-uri configurado.
     * Retorna un conjunto vacío si el issuer-uri está vacío o es un placeholder sin configurar.
     */
    public static Set<String> allowedFor(String issuerUri) {
        Set<String> allowed = new LinkedHashSet<>();
        if (issuerUri == null || issuerUri.isBlank() || issuerUri.contains("<TENANT_ID>")) {
            return allowed;
        }
        allowed.add(issuerUri);

        try {
            URI uri = URI.create(issuerUri);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String tenant = null;
            for (String segment : path.split("/")) {
                if (!segment.isBlank()) {
                    tenant = segment;
                    break;
                }
            }
            if (host != null && host.endsWith("login.microsoftonline.com") && tenant != null) {
                allowed.add("https://sts.windows.net/" + tenant + "/");
            }
        } catch (IllegalArgumentException ignored) {
            // issuer-uri mal formado: solo se acepta la coincidencia exacta
        }
        return allowed;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getClaimAsString("iss");
        if (issuer != null && allowedIssuers.contains(issuer)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Issuer ('iss') no permitido: " + issuer, null));
    }
}
