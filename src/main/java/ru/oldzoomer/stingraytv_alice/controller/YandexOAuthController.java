package ru.oldzoomer.stingraytv_alice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.oldzoomer.stingraytv_alice.service.TokenService;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * OAuth endpoints backed by TokenService (in-memory + JWT)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class YandexOAuthController {

    private final TokenService tokenService;

    @GetMapping("/oauth/authorize")
    public String authorizePage(
            @RequestParam String response_type,
            @RequestParam String client_id,
            @RequestParam(required = false) String scope,
            @RequestParam String state,
            @RequestParam(required = false) String redirect_uri,
            Model model
    ) {
        log.info("Authorize page request: client_id={}, response_type={}, scope={}", client_id, response_type, scope);

        // Validate OAuth parameters
        if (!"code".equalsIgnoreCase(response_type)) {
            throw new IllegalArgumentException("Unsupported response_type: " + response_type);
        }

        if (!tokenService.isValidClientRedirect(client_id, redirect_uri)) {
            throw new IllegalArgumentException("Invalid client_id or redirect_uri");
        }

        // No temporary code generation - standard OAuth flow

        // Pass OAuth parameters to the template
        model.addAttribute("state", state);
        model.addAttribute("client_id", client_id);
        model.addAttribute("redirect_uri", redirect_uri);
        model.addAttribute("scope", scope);

        // Return login page
        return "login";
    }

    @PostMapping("/oauth/login")
    public String login(
            @RequestParam String state,
            @RequestParam String client_id,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String scope,
            Model model
    ) {
        log.info("Login request: client_id={}, state={}", client_id, state);

        // Generate authorization code for anonymous user (no username/password)
        String redirectUrl = buildAuthorizationRedirectUrl(scope, state, client_id, redirect_uri);
        return "redirect:" + redirectUrl;
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> token(@RequestParam MultiValueMap<String, String> form) {
        String grantType = form.getFirst("grant_type");
        String clientId = form.getFirst("client_id");
        log.info("Token request grant_type={}, client_id={}", grantType, clientId);

        // Validate client_id for all grant types
        if (!tokenService.isValidClient(clientId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_client"));
        }

        return processTokenGrant(Objects.requireNonNull(grantType), form);
    }

    /**
     * Process different OAuth grant types
     */
    private ResponseEntity<Map<String, Object>> processTokenGrant(String grantType, MultiValueMap<String, String> form) {
        switch (grantType) {
            case "authorization_code":
                String code = form.getFirst("code");
                Optional<Map<String, Object>> codeResult = tokenService.consumeCode(code);
                return codeResult.map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "invalid_grant")));
            case "refresh_token":
                String refresh = form.getFirst("refresh_token");
                Optional<Map<String, Object>> refreshResult = tokenService.refresh(refresh);
                return refreshResult.map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "invalid_grant")));
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
        }
    }

    /**
     * Build authorization redirect URL after successful login
     */
    private String buildAuthorizationRedirectUrl(String scope, String state, String clientId, String redirectUri) {
        // Generate code for anonymous user (no username)
        String code = tokenService.createCode("anonymous", scope);

        String finalRedirectUri = redirectUri != null && !redirectUri.isBlank()
                ? redirectUri
                : "https://social.yandex.net/broker/redirect";

        StringBuilder redirectUrl = new StringBuilder(finalRedirectUri);
        redirectUrl.append("?code=").append(code);
        redirectUrl.append("&state=").append(state);
        redirectUrl.append("&client_id=").append(clientId);
        if (scope != null && !scope.isBlank()) {
            redirectUrl.append("&scope=").append(scope);
        }

        return redirectUrl.toString();
    }

}