package com.pedidos360.bff.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom JwtAuthenticationConverter para Microsoft Entra ID (Azure AD).
 * Entra ID enva los roles asignados a la aplicacin/usuario en el claim 'roles' (array de strings).
 * Spring Security por defecto busca 'scope' o 'scp', por lo que este convertidor mapea
 * cada rol del claim 'roles' a GrantedAuthority con el prefijo 'ROLE_'.
 */
@Component
public class RoleConverter extends JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public RoleConverter() {
        super();
        this.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    }

    /**
     * Extrae las autoridades desde el claim 'roles'.
     * Si el rol ya incluye 'ROLE_', lo mantiene; de lo contrario, lo antepone.
     */
    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        if (jwt == null) {
            return Collections.emptyList();
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
