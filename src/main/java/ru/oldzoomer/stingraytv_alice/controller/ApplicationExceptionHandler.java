package ru.oldzoomer.stingraytv_alice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.oldzoomer.stingraytv_alice.dto.yandex.YandexSmartHomeResponse;
import ru.oldzoomer.stingraytv_alice.service.YandexSmartHomeService;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApplicationExceptionHandler {
    private final YandexSmartHomeService smartHomeService;

    /**
     * Global exception handler for validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("validation-error");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Global exception handler for JSON parsing errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<YandexSmartHomeResponse> handleJsonParseException(HttpMessageNotReadableException ex) {
        log.warn("JSON parse error: {}", ex.getMessage());

        YandexSmartHomeResponse response = smartHomeService.createValidationErrorResponse("invalid-json");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Global exception handler for other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<YandexSmartHomeResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        YandexSmartHomeResponse response = smartHomeService.createInternalErrorResponse("internal-error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
