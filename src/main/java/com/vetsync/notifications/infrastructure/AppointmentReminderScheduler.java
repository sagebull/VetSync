package com.vetsync.notifications.infrastructure;

import com.vetsync.notifications.application.NotificationProperties;
import com.vetsync.notifications.application.ReminderProcessingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppointmentReminderScheduler {
    private final ReminderProcessingService reminders;
    private final NotificationProperties properties;

    public AppointmentReminderScheduler(ReminderProcessingService reminders, NotificationProperties properties) {
        this.reminders = reminders;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${vetsync.notifications.fixed-delay:PT1M}")
    public void processDueReminders() {
        if (properties.enabled()) {
            reminders.processDueReminders();
        }
    }
}
