package com.ecomm.sb_ecomm.payment.webhook;

public record WebhookPayload(
        String eventId ,
        String eventType,
        String gatewayReference,
        Long timestamp)
{}
