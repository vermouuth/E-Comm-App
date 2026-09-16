package com.ecomm.sb_ecomm.payment.webhook;

import org.springframework.web.bind.annotation.RequestHeader;

public interface WebhookService {

    void handle(byte[] body,@RequestHeader("X-Mock-Signature") String signature);
}
