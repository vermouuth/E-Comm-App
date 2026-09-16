package com.ecomm.sb_ecomm.gateway;

import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockGatewayServicesImpl implements MockGatewayServices {

   @Value("${mock.gateway.api-key}")
   private String configuredApiKey;

    @Override
    public GatewayInitResult initiate(Long paymentId, BigDecimal amount, String currency, String apiKey) {
        if(!apiKey.equals(this.configuredApiKey)){
            throw new ApiException("unAuthorized payment gateway request!");
        }
        String reference = "mock_" + UUID.randomUUID();
        String checkoutUrl = "/api/mock-gateway/checkout/" + reference;
        return new GatewayInitResult(reference, checkoutUrl);
    }
}
