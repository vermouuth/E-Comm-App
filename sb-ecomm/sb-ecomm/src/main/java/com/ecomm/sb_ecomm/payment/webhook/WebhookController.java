package com.ecomm.sb_ecomm.payment.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/mock")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<?> handle(
            @RequestBody byte[] body,
            @RequestHeader("X-Mock-Signature") String signature){
        webhookService.handle(body,signature);
        return ResponseEntity.ok().build();
    }
}
