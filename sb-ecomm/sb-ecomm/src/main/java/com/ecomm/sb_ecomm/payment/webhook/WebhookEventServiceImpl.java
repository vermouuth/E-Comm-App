package com.ecomm.sb_ecomm.payment.webhook;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WebhookEventServiceImpl  implements WebhookEventService {
    private final WebhookRepository repository;

    public WebhookEventServiceImpl(WebhookRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional (propagation = Propagation.REQUIRES_NEW)
    public void claim(String eventId) {
        try {
            WebhookEvent webhookEvent = new WebhookEvent();
            webhookEvent.setEventId(eventId);
            webhookEvent.setReceivedAt(Instant.now());
            repository.saveAndFlush(webhookEvent);
        }catch (DataIntegrityViolationException ex){
            throw new DuplicateWebhookEventException(eventId);
        }
    }
}
