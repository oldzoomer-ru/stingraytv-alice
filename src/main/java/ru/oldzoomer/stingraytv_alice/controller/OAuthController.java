package ru.oldzoomer.stingraytv_alice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.oldzoomer.stingraytv_alice.service.TemporaryCodeService;

import java.util.Map;

/**
 * OAuth2 controller for handling authorization flow with physical presence verification
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuthController {

    private final TemporaryCodeService temporaryCodeService;

    /**
     * Display login page with temporary code for physical presence authentication
     *
     * @param model    the model to add attributes to
     * @param clientId the client identifier
     * @return login page view
     */
    @GetMapping("/login")
    public String showLoginPage(Model model, @RequestParam String clientId,
                                @RequestParam String redirectUri, @RequestParam String state,
                                @RequestParam String scope) {
        log.info("Displaying login page for client: {}", clientId);

        // Generate a temporary code for this client
        temporaryCodeService.generateTemporaryCode(clientId);

        // Add the temporary code to the model so it can be displayed in the template
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("state", state);
        model.addAttribute("scope", scope);

        log.info("Generated temporary code for client {} and displaying login page", clientId);
        return "login";  // Thymeleaf template name
    }

    /**
     * Handle the form submission from the login page
     *
     * @param code     the temporary code entered by user
     * @param clientId the client identifier
     * @return redirect to authorization endpoint or error page
     */
    @PostMapping("/login")
    public ResponseEntity<Object> handleLogin(@RequestParam String code, @RequestParam String clientId,
                                              @RequestParam String redirectUri, @RequestParam String state,
                                              @RequestParam String scope) {
        log.info("Processing login with temporary code for client: {}", clientId);

        // Validate the temporary code
        if (temporaryCodeService.validateAndConsumeCode(code, clientId)) {
            log.info("Temporary code validated for client: {}", clientId);
            return ResponseEntity.status(302)
                    .header("Location", "/oauth/authorize?response_type=code&client_id=" + clientId +
                            "&redirect_uri=" + redirectUri + "&state=" + state + "&scope=" + scope)
                    .build();
        } else {
            // Invalid code
            log.warn("Invalid temporary code provided for client: {}", clientId);
            return ResponseEntity.status(400).body(Map.of("error", "Invalid temporary code"));
        }
    }
}
