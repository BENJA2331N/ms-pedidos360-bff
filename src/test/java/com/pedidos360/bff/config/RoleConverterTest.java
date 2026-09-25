package com.pedidos360.bff.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoleConverterTest {

    private RoleConverter roleConverter;

    @BeforeEach
    void setUp() {
        roleConverter = new RoleConverter();
    }

    @Test
    @DisplayName("Debe mapear roles de Entra ID agregando el prefijo ROLE_")
    void shouldMapRolesWithPrefix() {
        Jwt jwt = createJwtWithClaims(Map.of("roles", List.of("ADMIN", "OPERADOR")));

        Collection<GrantedAuthority> authorities = roleConverter.extractAuthorities(jwt);

        assertEquals(2, authorities.size());
        List<String> authorityNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorityNames.contains("ROLE_ADMIN"));
        assertTrue(authorityNames.contains("ROLE_OPERADOR"));
    }

    @Test
    @DisplayName("Debe respetar roles que ya tengan el prefijo ROLE_")
    void shouldNotDuplicatePrefixIfAlreadyPresent() {
        Jwt jwt = createJwtWithClaims(Map.of("roles", List.of("ROLE_CLIENTE")));

        Collection<GrantedAuthority> authorities = roleConverter.extractAuthorities(jwt);

        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE")));
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el JWT no tiene el claim roles")
    void shouldReturnEmptyListWhenNoRolesClaim() {
        Jwt jwt = createJwtWithClaims(Map.of("sub", "user123"));

        Collection<GrantedAuthority> authorities = roleConverter.extractAuthorities(jwt);

        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si jwt es nulo")
    void shouldReturnEmptyListWhenJwtIsNull() {
        Collection<GrantedAuthority> authorities = roleConverter.extractAuthorities(null);

        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    private Jwt createJwtWithClaims(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }
}
