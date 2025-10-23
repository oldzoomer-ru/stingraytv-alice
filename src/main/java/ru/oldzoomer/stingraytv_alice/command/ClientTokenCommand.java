package ru.oldzoomer.stingraytv_alice.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import ru.oldzoomer.stingraytv_alice.dto.ClientTokenDto;
import ru.oldzoomer.stingraytv_alice.service.ClientCredentialsService;
import ru.oldzoomer.stingraytv_alice.service.PreferencesStorageService;

import java.util.List;

/**
 * Spring Shell commands for managing client tokens in Preferences API
 */
@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class ClientTokenCommand {

    private final ClientCredentialsService clientCredentialsService;
    private final PreferencesStorageService preferencesStorageService;

    /**
     * List all client tokens stored in Preferences API
     */
    @ShellMethod(key = "client-tokens list", value = "List all client tokens stored in Preferences API")
    public String listClientTokens() {
        List<ClientTokenDto> tokens = preferencesStorageService.getAllClientTokens();

        if (tokens.isEmpty()) {
            return "No client tokens found in Preferences API";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Client tokens stored in Preferences API:\n\n");

        for (ClientTokenDto token : tokens) {
            sb.append(String.format("Client ID: %s\n", token.getClientId()));
            sb.append(String.format("  Created: %s\n", token.getCreatedAt()));
            sb.append(String.format("  Last Used: %s\n", token.getLastUsedAt()));
            sb.append(String.format("  Active: %s\n", token.isActive()));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Regenerate client credentials and store in Preferences API
     */
    @ShellMethod(key = "client-tokens regenerate", value = "Regenerate client credentials and store in Preferences API")
    public String regenerateClientTokens() {
        try {
            clientCredentialsService.regenerateCredentials();
            return "Client credentials regenerated and stored in Preferences API";
        } catch (Exception e) {
            log.error("Failed to regenerate client credentials", e);
            return "Failed to regenerate client credentials: " + e.getMessage();
        }
    }

    /**
     * Get client token details
     */
    @ShellMethod(key = "client-tokens get", value = "Get client token details")
    public String getClientToken(@ShellOption("Client ID") String clientId) {
        return preferencesStorageService.getClientToken(clientId)
                .map(token -> String.format(
                        """
                                Client Token Details:
                                  Client ID: %s
                                  Created: %s
                                  Last Used: %s
                                  Active: %s
                                """,
                        token.getClientId(),
                        token.getCreatedAt(),
                        token.getLastUsedAt(),
                        token.isActive()
                ))
                .orElse("Client token not found for ID: " + clientId);
    }

    /**
     * Delete client token from Preferences API
     */
    @ShellMethod(key = "client-tokens delete", value = "Delete client token from Preferences API")
    public String deleteClientToken(@ShellOption("Client ID") String clientId) {
        if (!preferencesStorageService.hasClientToken(clientId)) {
            return "Client token not found for ID: " + clientId;
        }

        preferencesStorageService.deleteClientToken(clientId);
        return "Client token deleted for ID: " + clientId;
    }

    /**
     * Show client credentials for OAuth2 usage
     */
    @ShellMethod(key = "client-credentials show", value = "Show client credentials for OAuth2 usage")
    public String showClientCredentials() {
        List<ClientTokenDto> tokens = preferencesStorageService.getAllClientTokens();

        if (tokens.isEmpty()) {
            return "No client credentials available. Run 'client-tokens regenerate' to generate them.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("OAuth2 Client Credentials:\n\n");

        for (ClientTokenDto token : tokens) {
            sb.append(String.format("Client ID: %s\n", token.getClientId()));
            sb.append(String.format("Client Secret: %s\n", token.getClientSecret()));
            sb.append("\n");
            sb.append("Usage:\n");
            sb.append("  Token endpoint: /oauth2/token\n");
            sb.append("  Grant type: client_credentials\n");
            sb.append("  Authentication: Basic Auth with client_id:client_secret\n");
            sb.append("\n");
        }

        return sb.toString();
    }
}