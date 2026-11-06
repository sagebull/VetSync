package com.vetsync.notifications.domain;

public record EmailMessage(
        String idempotencyKey,
        String recipientEmail,
        String subject,
        String body) {
}
