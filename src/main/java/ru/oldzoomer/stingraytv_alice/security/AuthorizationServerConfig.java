package ru.oldzoomer.stingraytv_alice.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import ru.oldzoomer.stingraytv_alice.service.ClientCredentialsService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

/**
 * OAuth2 Authorization Server Configuration
 * Implements RFC 6749 for OAuth2 Authorization Framework
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final ClientCredentialsService clientCredentialsService;
    private final OAuthProperties oAuthProperties;

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        log.info("Configuring OAuth2 Registered Clients");

        List<RegisteredClient> registeredClients = oAuthProperties.getClients().stream()
                .map(clientCredentialsService::createRegisteredClient)
                .toList();

        return new InMemoryRegisteredClientRepository(registeredClients);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        log.info("Generating RSA key pair for JWT signing");

        try {
            KeyPair keyPair = generateRsaKey();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();

            JWKSet jwkSet = new JWKSet(rsaKey);
            return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .authorizationEndpoint("/oauth/authorize")
                .tokenEndpoint("/oauth/token")
                .tokenIntrospectionEndpoint("/oauth/introspect")
                .tokenRevocationEndpoint("/oauth/revoke")
                .jwkSetEndpoint("/oauth/jwks")
                .oidcUserInfoEndpoint("/userinfo")
                .oidcClientRegistrationEndpoint("/connect/register")
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}