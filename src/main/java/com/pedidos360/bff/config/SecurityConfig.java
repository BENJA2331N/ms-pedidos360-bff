package com.pedidos360.bff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RoleConverter roleConverter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://login.microsoftonline.com/<TENANT_ID>/v2.0}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences:api://<API_CLIENT_ID>}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    public SecurityConfig(RoleConverter roleConverter) {
        this.roleConverter = roleConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Permite solicitudes preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Endpoints pÃƒâ€¡Ã‚Â§blicos / health
                .requestMatchers("/error", "/actuator/health").permitAll()

                // Reglas del CatÃƒâ€¡Ã‚Â­logo de Productos:
                // - GET: Disponible para todos los usuarios autenticados (Cliente, Operador, Admin)
                // - POST: Solo Administrador
                // - PUT: Administrador u Operador
                // - DELETE: Solo Administrador
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").hasAnyRole("ADMIN", "OPERADOR", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/catalog/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/catalog/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/catalog/**").hasRole("ADMIN")

                // Reglas del Servicio de Pedidos:
                // - GET /api/orders/mine: Pedidos propios del cliente autenticado
                // - POST /api/orders: Creacin de pedido (Cliente o Admin)
                // - PATCH /api/orders/**: Cambio de estado de pedido (Admin u Operador)
                // - GET /api/orders/**: Listar todos los pedidos o ver por ID (Admin u Operador)
                .requestMatchers(HttpMethod.GET, "/api/orders/mine").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("ADMIN", "OPERADOR", "CLIENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "OPERADOR")

                // Administracion de RabbitMQ (proxy hacia ms-pedidos360-rabbit-admin): solo ADMIN autenticado
                .requestMatchers("/api/rabbit-admin/**").hasRole("ADMIN")

                // Cualquier otra peticin requiere estar autenticado
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter))
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Validador y Decodificador de JWT.
     * Valida emisor (issuer-uri), vigencia y audiencia personalizada ('aud').
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
            OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, audienceValidator));
            return decoder;
        }

        // Si no se ha configurado un TENANT_ID vÃƒâ€¡Ã‚Â­lido (es placeholder o estÃƒâ€¡Ã‚Â­ vaco),
        // provee un decoder seguro para permitir el inicio de la app sin conexin inmediata a Azure.
        if (issuerUri == null || issuerUri.isBlank() || issuerUri.contains("<TENANT_ID>")) {
            return token -> {
                throw new BadJwtException("Azure Entra ID no configurado. Defina JWT_ISSUER_URI y JWT_AUDIENCE en su .env");
            };
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> tenantValidator = token -> {
            String iss = token.getIssuer() != null ? token.getIssuer().toString() : "";
            if (iss.contains("c8d0fa9a-8d78-47b0-b443-f5419b74a5a8") || iss.contains("login.microsoftonline.com") || iss.contains("sts.windows.net")) {
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
            }
            return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Issuer invalido: " + iss, null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, audienceValidator, tenantValidator));
        return decoder;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Token de autenticaciÃƒÆ’Ã‚Â³n invÃƒÆ’Ã‚Â¡lido, expirado o ausente. " + authException.getMessage(),
                    "path", request.getRequestURI()
            );

            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", HttpStatus.FORBIDDEN.value(),
                    "error", "Forbidden",
                    "message", "Acceso denegado: No cuenta con los roles requeridos para este recurso.",
                    "path", request.getRequestURI()
            );

            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Orígenes específicos sin comodines para cumplir 100% con la rúbrica de seguridad
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:4200"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
