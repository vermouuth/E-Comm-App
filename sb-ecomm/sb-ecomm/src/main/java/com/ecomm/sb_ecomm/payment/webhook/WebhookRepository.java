package com.ecomm.sb_ecomm.payment.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookRepository extends JpaRepository<WebhookEvent,Long> {

    /** 1 = first delivery of this event, 0 = already seen. Never throws on a duplicate, so the caller's transaction stays usable. */
    @Modifying
    @Query(value = "INSERT INTO webhook_events(event_id, received_at) VALUES (:id, now()) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int claim(@Param("id") String eventId);
}
