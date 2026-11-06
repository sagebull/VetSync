package com.vetsync.notifications.infrastructure;

import com.vetsync.notifications.domain.EmailMessage;
import com.vetsync.notifications.domain.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedEmailSender implements EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        LOGGER.info("Correo simulado procesado; clave de idempotencia={}", message.idempotencyKey());
    }
}
