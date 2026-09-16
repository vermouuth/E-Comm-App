package com.ecomm.sb_ecomm.payment.webhook;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "webhook_events",
uniqueConstraints = @UniqueConstraint(name = "uk_webhook_event_id" , columnNames = "event_id"))
@Data
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column
    private Instant receivedAt;


}
