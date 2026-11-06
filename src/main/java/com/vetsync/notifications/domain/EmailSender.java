package com.vetsync.notifications.domain;

public interface EmailSender {
    void send(EmailMessage message);
}
