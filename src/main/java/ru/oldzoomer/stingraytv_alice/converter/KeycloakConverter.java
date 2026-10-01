package ru.oldzoomer.stingraytv_alice.converter;

// Source - https://stackoverflow.com/a/75248599
// Posted by Ivan Tomić
// Retrieved 2025-11-29, License - CC BY-SA 4.0

// Edited and clean upped by Egor Gavrilov (@oldzoomer-ru on GitHub)

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts a Keycloak JWT into a Spring Security authentication token.
 * Extracts roles from both {@code realm_access.roles} and
 * {@code resource_access.<client_id>.roles} claims.
 */
@Component
public class KeycloakConverter implements Converter<@NonNull Jwt, AbstractAuthenticationToken> {

    // Shared ObjectMapper — thread-safe and expensive to create
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    @NullMarked
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        extractRealmRoles(jwt, authorities);
        extractResourceRoles(jwt, authorities);
        return authorities;
    }

    private void extractRealmRoles(Jwt jwt, List<GrantedAuthority> authorities) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return;
        }

        List<String> roles = MAPPER.convertValue(realmAccess.get("roles"), new TypeReference<>() {
        });

        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }
    }

    /**
     * Extracts roles from resource_access.<client_id>.roles claims.
     * Keycloak stores client-specific roles under resource_access,
     * distinct from realm-level roles in realm_access.
     */
    private void extractResourceRoles(Jwt jwt, List<GrantedAuthority> authorities) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return;
        }

        for (Object clientEntry : resourceAccess.values()) {
            if (clientEntry instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> clientData = (Map<String, Object>) clientEntry;
                List<String> roles = MAPPER.convertValue(clientData.get("roles"), new TypeReference<>() {
                });

                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            }
        }
    }
}
