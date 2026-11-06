package com.vetsync.notifications.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vetsync.notifications")
public record NotificationProperties(
        boolean enabled,
        @Positive long reminderMinutes,
        @NotNull Duration fixedDelay) {
    public NotificationProperties {
        if (reminderMinutes <= 0) {
            throw new IllegalArgumentException("La anticipacion del recordatorio debe ser positiva");
        }
        if (fixedDelay == null || fixedDelay.isZero() || fixedDelay.isNegative()) {
            throw new IllegalArgumentException("El intervalo del programador debe ser positivo");
        }
    }
}
