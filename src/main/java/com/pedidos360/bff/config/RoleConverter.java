package com.pedidos360.bff.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Custom JwtAuthenticationConverter para Microsoft Entra ID (Azure AD).
 * Entra ID envía los roles asignados a la aplicación/usuario en el claim 'roles' (array de strings)
 * y los permisos delegados en el claim 'scp' (string separado por espacios).
 * Spring Security por defecto solo busca 'scope' o 'scp', por lo que este convertidor mapea
 * cada rol del claim 'roles' a GrantedAuthority con el prefijo 'ROLE_' y cada scope de 'scp'
 * con el prefijo 'SCOPE_'.
 */
@Component
public class RoleConverter extends JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public RoleConverter() {
        super();
        this.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    }

    /**
     * Extrae las autoridades desde los claims 'roles' (ROLE_...) y 'scp' (SCOPE_...).
     * Si el rol ya incluye 'ROLE_', lo mantiene; de lo contrario, lo antepone.
     * El claim 'scp' de Entra ID es un string separado por espacios (ej. "access_as_user").
     */
    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        if (jwt == null) {
            return Collections.emptyList();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        String scp = jwt.getClaimAsString("scp");
        if (scp != null && !scp.isBlank()) {
            Arrays.stream(scp.trim().split("\\s+"))
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .forEach(authorities::add);
        }

        return authorities;
    }
}
