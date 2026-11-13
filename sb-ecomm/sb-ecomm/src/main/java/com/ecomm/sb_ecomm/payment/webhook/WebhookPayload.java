package com.ecomm.sb_ecomm.payment.webhook;

public record WebhookPayload(
        String eventId ,
        String eventType,
        String gatewayReference,
        Long timestamp)
{
    // The only event types the gateway sends; MockGatewayController and WebhookServiceImpl share them.
    public static final String CAPTURE = "CAPTURE";
    public static final String FAILED = "FAILED";
}
