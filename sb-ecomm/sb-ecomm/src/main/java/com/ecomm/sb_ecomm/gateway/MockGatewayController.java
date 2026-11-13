package com.ecomm.sb_ecomm.gateway;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.payment.webhook.SignatureUtil;
import com.ecomm.sb_ecomm.payment.webhook.WebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.UUID;

// Can sign and fire "payment captured" webhooks, so it only exists when mock.gateway.enabled=true.
@ConditionalOnProperty(name = "mock.gateway.enabled", havingValue = "true")
@Hidden
@Validated
@RestController
@RequestMapping("/api/mock-gateway")
public class MockGatewayController {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Value("${mock.gateway.signing-secret}")
    private String signingSecret;

    @Value("${server.port}")
    private String port;

    @PostMapping("/complete/{gatewayReference}")
    public ResponseEntity<?> completePayment(
            @PathVariable String gatewayReference,
            @NotBlank(message = "outcome is required") @RequestParam String outcome) throws Exception
    {
        String eventType = switch (outcome.toUpperCase()){
            case "CAPTURED" -> WebhookPayload.CAPTURE;
            case "FAILED" -> WebhookPayload.FAILED;
            default ->  throw new IllegalArgumentException("Invalid outcome: " + outcome);
        };

        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),
                eventType,
                gatewayReference,
                System.currentTimeMillis() / 1000
        );

        byte[] body = objectMapper.writer().writeValueAsBytes(payload);
        String signature = SignatureUtil.hmacSha256(body, signingSecret);

        restClient.post()
                .uri("http://localhost:"+port+"/api/webhooks/mock")
                .header("X-Mock-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        return ResponseEntity.ok("Webhook fired: " + eventType + "for " + gatewayReference);
    }
}
