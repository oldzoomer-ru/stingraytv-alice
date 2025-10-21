package ru.oldzoomer.stingraytv_alice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StingraytvAliceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StingraytvAliceApplication.class, args);
    }

}
