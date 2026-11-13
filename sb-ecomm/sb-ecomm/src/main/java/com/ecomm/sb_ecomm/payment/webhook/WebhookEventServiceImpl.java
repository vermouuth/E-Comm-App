package com.ecomm.sb_ecomm.payment.webhook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEventServiceImpl  implements WebhookEventService {
    private final WebhookRepository repository;

    public WebhookEventServiceImpl(WebhookRepository repository) {
        this.repository = repository;
    }

    // MANDATORY: the claim must commit or roll back together with the work it guards.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(String eventId) {
        return repository.claim(eventId) == 1;
    }
}
