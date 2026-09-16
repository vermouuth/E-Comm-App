package com.ecomm.sb_ecomm.payment.webhook;

public class DuplicateWebhookEventException extends RuntimeException {
    public DuplicateWebhookEventException(String eventId) {
        super("Duplicate webhook event: " + eventId);
    }
}
