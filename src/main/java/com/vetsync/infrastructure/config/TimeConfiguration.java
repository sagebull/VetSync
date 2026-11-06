package com.vetsync.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    Clock applicationClock(@Value("${vetsync.scheduling.zone:UTC}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
