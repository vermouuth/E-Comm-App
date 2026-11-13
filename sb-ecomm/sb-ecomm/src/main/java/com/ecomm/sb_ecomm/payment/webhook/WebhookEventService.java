package com.ecomm.sb_ecomm.payment.webhook;

public interface WebhookEventService {
    /** true if this is the first delivery of the event; joins the caller's transaction so a failed handling un-claims it. */
    boolean claim(String eventId);
}
