package com.ecomm.sb_ecomm.payment.webhook;

import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import com.ecomm.sb_ecomm.payment.repository.PaymentRepository;
import com.ecomm.sb_ecomm.payment.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);
    private static final long MAX_AGE_SECONDS = 300;

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final WebhookEventService webhookEventService;

    @Value("${mock.gateway.signing-secret}")
    private String signingSecret;

    public WebhookServiceImpl(
            ObjectMapper objectMapper,
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            WebhookEventService webhookEventService
    ) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.webhookEventService = webhookEventService;
    }

    @Override
    @Transactional
    public void handle(byte[] body, String signature) {

        verifySignature(body, signature);

        WebhookPayload event = parsePayload(body);

        validateTimestamp(event);

        if (isDuplicate(event)) {
            return;
        }

        Long paymentId = resolvePaymentId(event);

        PaymentStatus targetStatus = mapEventToStatus(event);

        updatePaymentStatus(paymentId, targetStatus);

    }

    private void verifySignature(byte[] body, String signature) {

        String expected = SignatureUtil.hmacSha256(body, signingSecret);

        if (!SignatureUtil.constantTimeEquals(expected, signature)) {
            throw new ApiException("Invalid webhook signature");
        }
    }

    private WebhookPayload parsePayload(byte[] body) {

        try {
            return objectMapper.readValue(body, WebhookPayload.class);
        } catch (Exception e) {
            throw new ApiException("Malformed webhook body");
        }
    }

    private void validateTimestamp(WebhookPayload event) {

        long ageSeconds =
                (System.currentTimeMillis() / 1000) - event.timestamp();

        if (ageSeconds > MAX_AGE_SECONDS) {
            throw new ApiException("Webhook timestamp too old.");
        }
    }

    private boolean isDuplicate(WebhookPayload event) {

        try {
            webhookEventService.claim(event.eventId());
            return false;

        } catch (DuplicateWebhookEventException dup) {

            log.info("Duplicate webhook {} ignored", event.eventId());
            return true;
        }
    }

    private Long resolvePaymentId(WebhookPayload event) {

        return paymentRepository
                .findByPgPaymentId(event.gatewayReference())
                .orElseThrow(() ->
                        new ApiException(
                                "No payment for reference: "
                                        + event.gatewayReference()
                        )
                )
                .getId();
    }

    private PaymentStatus mapEventToStatus(WebhookPayload event) {

        return switch (event.eventType()) {

            case "CAPTURE" -> PaymentStatus.CAPTURED;

            case "FAILED" -> PaymentStatus.FAILED;

            default -> {
                log.info(
                        "Unhandled webhook type {}",
                        event.eventType()
                );
                yield null;
            }
        };
    }

    private void updatePaymentStatus(
            Long paymentId,
            PaymentStatus targetStatus
    ) {

        if (targetStatus != null) {
            paymentService.updatePaymentStatus(
                    paymentId,
                    targetStatus
            );
        }
    }
}
