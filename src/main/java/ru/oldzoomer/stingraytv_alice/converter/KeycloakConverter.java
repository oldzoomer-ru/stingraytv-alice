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

@Component
public class KeycloakConverter implements Converter<@NonNull Jwt, AbstractAuthenticationToken> {

    @Override
    @NullMarked
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        if (jwt.getClaim("realm_access") != null) {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            ObjectMapper mapper = new ObjectMapper();
            List<String> roles = mapper.convertValue(realmAccess.get("roles"), new TypeReference<>() {
            });
            List<GrantedAuthority> authorities = new ArrayList<>();

            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority(role));
            }

            return authorities;
        }
        return new ArrayList<>();
    }
}


