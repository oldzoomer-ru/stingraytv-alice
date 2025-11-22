package ru.oldzoomer.stingraytv_alice.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.oldzoomer.stingraytv_alice.service.TemporaryCodeService;

import java.util.concurrent.TimeUnit;

@EnableScheduling
@RequiredArgsConstructor
@Component
public class CodeCleanupSchedule {
    private final TemporaryCodeService temporaryCodeService;

    // Schedule a task to clean up expired codes every 10 minutes
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredCodes() {
        temporaryCodeService.cleanupExpiredCodes();
    }
}
