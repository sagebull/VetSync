package com.vetsync.notifications.infrastructure;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vetsync.notifications.application.NotificationProperties;
import com.vetsync.notifications.application.ReminderProcessingService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderSchedulerTest {
    @Mock
    private ReminderProcessingService reminders;

    @Test
    void processesRemindersWhenEnabled() {
        AppointmentReminderScheduler scheduler = new AppointmentReminderScheduler(reminders,
                new NotificationProperties(true, 60, Duration.ofMinutes(1)));

        scheduler.processDueReminders();

        verify(reminders).processDueReminders();
    }

    @Test
    void skipsProcessingWhenDisabled() {
        AppointmentReminderScheduler scheduler = new AppointmentReminderScheduler(reminders,
                new NotificationProperties(false, 60, Duration.ofMinutes(1)));

        scheduler.processDueReminders();

        verify(reminders, never()).processDueReminders();
    }
}
