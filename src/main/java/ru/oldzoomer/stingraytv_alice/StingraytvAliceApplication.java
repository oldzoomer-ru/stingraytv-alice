package ru.oldzoomer.stingraytv_alice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for StingrayTV Alice integration.
 * This Spring Boot application provides integration with Yandex Smart Home API
 * for controlling StingrayTV receivers.
 */
@SpringBootApplication
public class StingraytvAliceApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(StingraytvAliceApplication.class, args);
    }

}
