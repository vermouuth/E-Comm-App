package com.ecomm.sb_ecomm.payment.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEvent,Long> {
}
